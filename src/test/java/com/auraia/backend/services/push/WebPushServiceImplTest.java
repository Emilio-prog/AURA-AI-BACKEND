package com.auraia.backend.services.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.dto.request.PushRequests;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserAchievement;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.entities.WebPushDelivery;
import com.auraia.backend.models.entities.WebPushSubscription;
import com.auraia.backend.models.enums.AchievementCode;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.models.enums.WebPushDeliveryStatus;
import com.auraia.backend.models.enums.WebPushNotificationType;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.repositories.WebPushDeliveryRepository;
import com.auraia.backend.repositories.WebPushSubscriptionRepository;
import com.auraia.backend.security.UserPrincipal;
import com.auraia.backend.services.privacy.TestContentCryptoService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WebPushServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    DiaryEntryRepository diaryEntryRepository;
    @Mock
    MoodLogRepository moodLogRepository;
    @Mock
    UserAchievementRepository userAchievementRepository;
    @Mock
    WebPushSubscriptionRepository subscriptionRepository;
    @Mock
    WebPushDeliveryRepository deliveryRepository;
    @Mock
    WebPushSender webPushSender;

    AppProperties properties;
    WebPushServiceImpl service;
    User user;
    UserSettings settings;
    WebPushSubscription subscription;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getWebPush().setEnabled(true);
        properties.getWebPush().setVapidPublicKey("public-key");
        properties.getWebPush().setVapidPrivateKey("private-key");
        properties.getWebPush().setDiaryReminderTime("21:30");

        service = new WebPushServiceImpl(
            properties,
            userRepository,
            userSettingsRepository,
            diaryEntryRepository,
            moodLogRepository,
            userAchievementRepository,
            subscriptionRepository,
            deliveryRepository,
            new TestContentCryptoService(true),
            webPushSender
        );

        UUID userId = UUID.randomUUID();
        user = User.builder()
            .email("push@example.com")
            .passwordHash("hash")
            .name("Push User")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .onboardedAt(Instant.parse("2026-05-10T10:00:00Z"))
            .build();
        user.setId(userId);

        settings = UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .notificationPreferences(Map.of(
                "enabled", true,
                "dailyReminderTime", "20:00",
                "moodReminderEnabled", true,
                "streakEnabled", true
            ))
            .build();

        subscription = WebPushSubscription.builder()
            .user(user)
            .endpoint("auraenc:v1:test:https://push.example/sub")
            .endpointHash("hash")
            .p256dh("auraenc:v1:test:p256dh")
            .authSecret("auraenc:v1:test:auth")
            .active(true)
            .build();
        subscription.setId(UUID.randomUUID());
        subscription.setCreatedAt(Instant.parse("2026-05-12T08:00:00Z"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
        lenient().when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        lenient().when(subscriptionRepository.save(any(WebPushSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(deliveryRepository.save(any(WebPushDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void subscribeStoresEncryptedSubscriptionAndIsIdempotentByEndpointHash() {
        when(subscriptionRepository.findByUserAndEndpointHash(any(), any())).thenReturn(Optional.empty());

        service.subscribe(new PushRequests.SubscriptionRequest(
            "https://push.example/sub",
            null,
            new PushRequests.SubscriptionKeys("p256dh", "auth")
        ));

        ArgumentCaptor<WebPushSubscription> captor = ArgumentCaptor.forClass(WebPushSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        WebPushSubscription saved = captor.getValue();
        assertThat(saved.getEndpoint()).startsWith("auraenc:v1:test:");
        assertThat(saved.getP256dh()).startsWith("auraenc:v1:test:");
        assertThat(saved.getAuthSecret()).startsWith("auraenc:v1:test:");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void configReturnsPublicKeyAndCurrentSubscriptionState() {
        when(subscriptionRepository.existsByUserAndActiveTrue(user)).thenReturn(true);

        var response = service.config();

        assertThat(response.enabled()).isTrue();
        assertThat(response.publicKey()).isEqualTo("public-key");
        assertThat(response.subscribed()).isTrue();
    }

    @Test
    void schedulerDoesNotSendMoodReminderWhenMoodAlreadyExistsToday() {
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(subscription));
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(moodLogRepository.existsByUserAndLoggedAtBetween(any(), any(), any())).thenReturn(true);

        service.runScheduledReminders(Instant.parse("2026-05-12T18:00:00Z"));

        verify(webPushSender, never()).send(any(), any());
    }

    @Test
    void schedulerSendsDiaryReminderOncePerSubscriptionAndDay() {
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(subscription));
        when(subscriptionRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(subscription));
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(webPushSender.send(any(), any())).thenReturn(WebPushSendResult.success(201));

        service.runScheduledReminders(Instant.parse("2026-05-12T19:30:00Z"));

        ArgumentCaptor<WebPushDelivery> captor = ArgumentCaptor.forClass(WebPushDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(WebPushNotificationType.DIARY_REMINDER);
        assertThat(captor.getValue().getTargetKey()).isEqualTo("diary:2026-05-12");
        assertThat(captor.getValue().getStatus()).isEqualTo(WebPushDeliveryStatus.SENT);
    }

    @Test
    void testNotificationRevokesSubscriptionWhenProviderReturnsGone() {
        when(subscriptionRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(subscription));
        when(webPushSender.send(any(), any())).thenReturn(WebPushSendResult.failure(410, "Gone"));

        var response = service.test();

        assertThat(response.sent()).isFalse();
        assertThat(subscription.isActive()).isFalse();
        assertThat(subscription.getRevokedAt()).isNotNull();
        ArgumentCaptor<WebPushDelivery> captor = ArgumentCaptor.forClass(WebPushDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WebPushDeliveryStatus.SUBSCRIPTION_REVOKED);
    }

    @Test
    void schedulerGroupsNewAchievementsInOnePushAndRecordsEachCode() {
        UserAchievement first = achievement(AchievementCode.REFUGIO_ACTIVADO, "2026-05-12T09:00:00Z");
        UserAchievement second = achievement(AchievementCode.PRIMER_CHECKIN_MOOD, "2026-05-12T09:02:00Z");
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(subscription));
        when(subscriptionRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(subscription));
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(userAchievementRepository.findByUserOrderByUnlockedAtAsc(user)).thenReturn(List.of(first, second));
        when(webPushSender.send(any(), any())).thenReturn(WebPushSendResult.success(201));

        service.runScheduledReminders(Instant.parse("2026-05-12T10:15:00Z"));

        ArgumentCaptor<WebPushDelivery> captor = ArgumentCaptor.forClass(WebPushDelivery.class);
        verify(webPushSender).send(any(), any());
        verify(deliveryRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(WebPushDelivery::getTargetKey)
            .containsExactlyInAnyOrder("achievement:REFUGIO_ACTIVADO", "achievement:PRIMER_CHECKIN_MOOD");
    }

    private UserAchievement achievement(AchievementCode code, String unlockedAt) {
        UserAchievement achievement = UserAchievement.builder()
            .user(user)
            .code(code)
            .unlockedAt(Instant.parse(unlockedAt))
            .build();
        achievement.setId(UUID.randomUUID());
        achievement.setCreatedAt(Instant.parse(unlockedAt));
        return achievement;
    }
}
