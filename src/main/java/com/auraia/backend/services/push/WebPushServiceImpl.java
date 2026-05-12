package com.auraia.backend.services.push;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.PushRequests;
import com.auraia.backend.models.dto.response.PushResponses;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserAchievement;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.entities.WebPushDelivery;
import com.auraia.backend.models.entities.WebPushSubscription;
import com.auraia.backend.models.enums.WebPushDeliveryStatus;
import com.auraia.backend.models.enums.WebPushNotificationType;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.repositories.WebPushDeliveryRepository;
import com.auraia.backend.repositories.WebPushSubscriptionRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.privacy.ContentCryptoService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebPushServiceImpl implements WebPushService {

    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Europe/Madrid");
    private static final String DEFAULT_MOOD_REMINDER_TIME = "20:00";

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final MoodLogRepository moodLogRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final WebPushSubscriptionRepository subscriptionRepository;
    private final WebPushDeliveryRepository deliveryRepository;
    private final ContentCryptoService cryptoService;
    private final WebPushSender webPushSender;

    @Override
    @Transactional(readOnly = true)
    public PushResponses.PushConfigResponse config() {
        User user = currentUser();
        boolean enabled = properties.getWebPush().isEnabled();
        String publicKey = enabled ? properties.getWebPush().getVapidPublicKey() : null;
        return new PushResponses.PushConfigResponse(enabled, publicKey, subscriptionRepository.existsByUserAndActiveTrue(user));
    }

    @Override
    @Transactional
    public PushResponses.PushSubscriptionResponse subscribe(PushRequests.SubscriptionRequest request) {
        ensureEnabled();
        User user = currentUser();
        String endpoint = request.endpoint().trim();
        String hash = endpointHash(endpoint);
        WebPushSubscription subscription = subscriptionRepository.findByUserAndEndpointHash(user, hash)
            .orElseGet(() -> WebPushSubscription.builder()
                .user(user)
                .endpointHash(hash)
                .build());
        subscription.setEndpoint(cryptoService.encrypt(user.getId(), "push.endpoint", endpoint));
        subscription.setP256dh(cryptoService.encrypt(user.getId(), "push.p256dh", request.keys().p256dh().trim()));
        subscription.setAuthSecret(cryptoService.encrypt(user.getId(), "push.auth", request.keys().auth().trim()));
        subscription.setExpirationTime(request.expirationTime());
        subscription.setActive(true);
        subscription.setRevokedAt(null);
        subscription.setFailureReason(null);
        subscriptionRepository.save(subscription);
        return new PushResponses.PushSubscriptionResponse(true);
    }

    @Override
    @Transactional
    public PushResponses.PushSubscriptionResponse disable(PushRequests.DisableSubscriptionRequest request) {
        User user = currentUser();
        List<WebPushSubscription> subscriptions = activeSubscriptions(user);
        if (request.endpoint() != null && !request.endpoint().isBlank()) {
            String hash = endpointHash(request.endpoint().trim());
            subscriptions = subscriptions.stream()
                .filter(subscription -> subscription.getEndpointHash().equals(hash))
                .toList();
        }
        Instant now = Instant.now();
        for (WebPushSubscription subscription : subscriptions) {
            subscription.setActive(false);
            subscription.setRevokedAt(now);
            subscription.setFailureReason("disabled_by_user");
            subscriptionRepository.save(subscription);
        }
        return new PushResponses.PushSubscriptionResponse(subscriptionRepository.existsByUserAndActiveTrue(user));
    }

    @Override
    @Transactional
    public PushResponses.PushTestResponse test() {
        ensureEnabled();
        User user = currentUser();
        List<WebPushSubscription> subscriptions = activeSubscriptions(user);
        if (subscriptions.isEmpty()) {
            throw new BusinessException("error.push_subscription_required");
        }
        WebPushPayload payload = new WebPushPayload(
            WebPushNotificationType.TEST,
            "AURA IA",
            "Notificaciones activadas correctamente.",
            "/#/dashboard/config"
        );
        boolean sent = false;
        String targetKey = "test:" + Instant.now().toEpochMilli();
        for (WebPushSubscription subscription : subscriptions) {
            sent = sendAndRecord(user, subscription, WebPushNotificationType.TEST, targetKey, payload) || sent;
        }
        return new PushResponses.PushTestResponse(sent);
    }

    @Override
    @Transactional
    public void runScheduledReminders(Instant now) {
        if (!properties.getWebPush().isEnabled() || !properties.getWebPush().isSchedulerEnabled()) {
            return;
        }
        Map<UUID, User> users = subscriptionRepository.findByActiveTrue().stream()
            .map(WebPushSubscription::getUser)
            .filter(user -> user.getDeletedAt() == null && user.getOnboardedAt() != null)
            .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        for (User user : users.values()) {
            UserSettings settings = settingsFor(user);
            ZoneId zone = userZone(settings);
            Map<String, Object> preferences = settings.getNotificationPreferences();
            if (!notificationsEnabled(preferences)) {
                continue;
            }
            sendMoodReminderIfDue(user, preferences, zone, now);
            sendDiaryReminderIfDue(user, zone, now);
            sendAchievementNotifications(user);
        }
    }

    private void sendMoodReminderIfDue(User user, Map<String, Object> preferences, ZoneId zone, Instant now) {
        if (!booleanPreference(preferences, "moodReminderEnabled", true)) {
            return;
        }
        LocalTime reminderTime = parseTime(stringPreference(preferences, "dailyReminderTime", DEFAULT_MOOD_REMINDER_TIME), DEFAULT_MOOD_REMINDER_TIME);
        if (!isDue(now, zone, reminderTime)) {
            return;
        }
        DayWindow day = dayWindow(now, zone);
        if (moodLogRepository.existsByUserAndLoggedAtBetween(user, day.start(), day.end())) {
            return;
        }
        WebPushPayload payload = new WebPushPayload(
            WebPushNotificationType.MOOD_REMINDER,
            "AURA IA",
            "Tienes un recordatorio pendiente.",
            "/#/dashboard/mood"
        );
        sendToActiveSubscriptions(user, WebPushNotificationType.MOOD_REMINDER, "mood:" + day.date(), payload);
    }

    private void sendDiaryReminderIfDue(User user, ZoneId zone, Instant now) {
        LocalTime reminderTime = parseTime(properties.getWebPush().getDiaryReminderTime(), "21:30");
        if (!isDue(now, zone, reminderTime)) {
            return;
        }
        DayWindow day = dayWindow(now, zone);
        if (diaryEntryRepository.existsByUserAndCreatedAtBetween(user, day.start(), day.end())) {
            return;
        }
        WebPushPayload payload = new WebPushPayload(
            WebPushNotificationType.DIARY_REMINDER,
            "AURA IA",
            "Tu espacio de diario sigue disponible hoy.",
            "/#/dashboard/diario"
        );
        sendToActiveSubscriptions(user, WebPushNotificationType.DIARY_REMINDER, "diary:" + day.date(), payload);
    }

    private void sendAchievementNotifications(User user) {
        List<WebPushSubscription> subscriptions = activeSubscriptions(user);
        if (subscriptions.isEmpty()) {
            return;
        }
        List<UserAchievement> achievements = userAchievementRepository.findByUserOrderByUnlockedAtAsc(user);
        for (WebPushSubscription subscription : subscriptions) {
            List<UserAchievement> pending = achievements.stream()
                .filter(achievement -> !achievement.getUnlockedAt().isBefore(subscription.getCreatedAt()))
                .filter(achievement -> !deliveryRepository.existsByUserAndSubscriptionAndNotificationTypeAndTargetKey(
                    user,
                    subscription,
                    WebPushNotificationType.ACHIEVEMENT_UNLOCKED,
                    "achievement:" + achievement.getCode().name()
                ))
                .toList();
            if (pending.isEmpty()) {
                continue;
            }
            WebPushPayload payload = new WebPushPayload(
                WebPushNotificationType.ACHIEVEMENT_UNLOCKED,
                "AURA IA",
                "Has desbloqueado nuevos logros.",
                "/#/dashboard/logros"
            );
            WebPushSendResult result = webPushSender.send(subscription, payload);
            Instant sentAt = Instant.now();
            for (UserAchievement achievement : pending) {
                saveDelivery(user, subscription, WebPushNotificationType.ACHIEVEMENT_UNLOCKED,
                    "achievement:" + achievement.getCode().name(), payload, result, sentAt);
            }
            updateSubscription(subscription, result, sentAt);
        }
    }

    private void sendToActiveSubscriptions(User user,
                                           WebPushNotificationType type,
                                           String targetKey,
                                           WebPushPayload payload) {
        for (WebPushSubscription subscription : activeSubscriptions(user)) {
            sendAndRecord(user, subscription, type, targetKey, payload);
        }
    }

    private boolean sendAndRecord(User user,
                                  WebPushSubscription subscription,
                                  WebPushNotificationType type,
                                  String targetKey,
                                  WebPushPayload payload) {
        if (deliveryRepository.existsByUserAndSubscriptionAndNotificationTypeAndTargetKey(user, subscription, type, targetKey)) {
            return false;
        }
        WebPushSendResult result = webPushSender.send(subscription, payload);
        Instant sentAt = Instant.now();
        saveDelivery(user, subscription, type, targetKey, payload, result, sentAt);
        updateSubscription(subscription, result, sentAt);
        return result.success();
    }

    private void saveDelivery(User user,
                              WebPushSubscription subscription,
                              WebPushNotificationType type,
                              String targetKey,
                              WebPushPayload payload,
                              WebPushSendResult result,
                              Instant sentAt) {
        WebPushDeliveryStatus status = result.success()
            ? WebPushDeliveryStatus.SENT
            : result.subscriptionRevoked() ? WebPushDeliveryStatus.SUBSCRIPTION_REVOKED : WebPushDeliveryStatus.FAILED;
        deliveryRepository.save(WebPushDelivery.builder()
            .user(user)
            .subscription(subscription)
            .notificationType(type)
            .targetKey(targetKey)
            .payloadJson(payload.toMap())
            .status(status)
            .providerStatus(result.statusCode() == 0 ? null : result.statusCode())
            .errorMessage(result.errorMessage())
            .sentAt(result.success() ? sentAt : null)
            .build());
    }

    private void updateSubscription(WebPushSubscription subscription, WebPushSendResult result, Instant now) {
        if (result.success()) {
            subscription.setLastSuccessAt(now);
            subscription.setLastFailureAt(null);
            subscription.setFailureReason(null);
        } else {
            subscription.setLastFailureAt(now);
            subscription.setFailureReason(result.errorMessage());
            if (result.subscriptionRevoked()) {
                subscription.setActive(false);
                subscription.setRevokedAt(now);
            }
        }
        subscriptionRepository.save(subscription);
    }

    private List<WebPushSubscription> activeSubscriptions(User user) {
        return subscriptionRepository.findByUserAndActiveTrue(user);
    }

    private UserSettings settingsFor(User user) {
        return userSettingsRepository.findByUser(user).orElseGet(() -> UserSettings.builder()
            .user(user)
            .theme(com.auraia.backend.models.enums.Theme.SYSTEM)
            .language("es")
            .timezone(FALLBACK_ZONE.getId())
            .notificationPreferences(new LinkedHashMap<>())
            .build());
    }

    private boolean notificationsEnabled(Map<String, Object> preferences) {
        return booleanPreference(preferences, "enabled", true);
    }

    private boolean booleanPreference(Map<String, Object> preferences, String key, boolean fallback) {
        if (preferences == null || !preferences.containsKey(key)) {
            return fallback;
        }
        Object value = preferences.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private String stringPreference(Map<String, Object> preferences, String key, String fallback) {
        if (preferences == null) {
            return fallback;
        }
        Object value = preferences.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private ZoneId userZone(UserSettings settings) {
        try {
            return ZoneId.of(settings.getTimezone());
        } catch (DateTimeException | NullPointerException ex) {
            return FALLBACK_ZONE;
        }
    }

    private boolean isDue(Instant now, ZoneId zone, LocalTime reminderTime) {
        LocalTime local = now.atZone(zone).toLocalTime();
        return local.getHour() == reminderTime.getHour() && local.getMinute() == reminderTime.getMinute();
    }

    private DayWindow dayWindow(Instant now, ZoneId zone) {
        LocalDate date = now.atZone(zone).toLocalDate();
        Instant start = date.atStartOfDay(zone).toInstant();
        return new DayWindow(date, start, date.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private LocalTime parseTime(String value, String fallback) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException ex) {
            return LocalTime.parse(fallback);
        }
    }

    private String endpointHash(String endpoint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(endpoint.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureEnabled() {
        if (!properties.getWebPush().isEnabled()) {
            throw new BusinessException("error.push_unavailable");
        }
    }

    private record DayWindow(LocalDate date, Instant start, Instant end) {
    }
}
