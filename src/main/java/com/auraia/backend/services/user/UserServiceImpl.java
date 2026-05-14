package com.auraia.backend.services.user;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.mappers.UserSettingsMapper;
import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.EmailVerificationToken;
import com.auraia.backend.models.entities.PanicAlert;
import com.auraia.backend.models.entities.PanicNotificationResult;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.EmailVerificationTokenRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.PanicAlertRepository;
import com.auraia.backend.repositories.PanicNotificationResultRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.UserDeletionService;
import com.auraia.backend.services.auth.PasswordPolicyValidator;
import com.auraia.backend.services.auth.VerificationEmailService;
import com.auraia.backend.services.privacy.ContentCryptoService;
import com.auraia.backend.utils.TokenHashing;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ONBOARDING_CONSENT_VERSION = "privacy:2026-05-10;terms:2026-05-10;onboarding:v1";

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final MoodLogRepository moodLogRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ContactRepository contactRepository;
    private final PanicAlertRepository panicAlertRepository;
    private final PanicNotificationResultRepository notificationResultRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final UserDeletionService userDeletionService;
    private final VerificationEmailService verificationEmailService;
    private final AppProperties properties;
    private final ContentCryptoService contentCryptoService;
    private final UserExportPdfService userExportPdfService;

    @Override
    @Transactional
    public UserResponses.UserResponse getCurrentProfile() {
        User user = currentUser();
        protectOnboardingProfile(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponses.UserResponse updateCurrentProfile(UserRequests.UpdateUserRequest request) {
        User user = currentUser();
        boolean emailChanged = false;
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new BusinessException("error.email_in_use");
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                user.setEmailVerified(false);
                emailChanged = true;
            }
        }
        User saved = userRepository.save(user);
        if (emailChanged) {
            createAndSendVerificationToken(saved);
        }
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponses.UserResponse completeOnboarding(UserRequests.CompleteOnboardingRequest request) {
        User user = currentUser();
        if (user.getOnboardedAt() != null) {
            return userMapper.toResponse(user);
        }

        validateOnboarding(request);

        String language = request.language().trim().toLowerCase(Locale.ROOT);
        String timezone = request.timezone().trim();
        Instant now = Instant.now();

        user.setName(request.preferredName().trim());
        user.setOnboardedAt(now);
        user.setOnboardingConsentAt(now);
        user.setOnboardingConsentVersion(ONBOARDING_CONSENT_VERSION);
        user.setOnboardingProfile(contentCryptoService.encryptJsonMap(user.getId(), "user.onboarding-profile", onboardingProfile(request)));

        updateSettings(user, language, timezone, request.notifications());
        createTrustedContactIfPresent(user, request.trustedContact());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse changePassword(UserRequests.ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("error.current_password");
        }
        passwordPolicyValidator.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new AuthResponses.MessageResponse("OK");
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse deleteCurrentAccount(UserRequests.DeleteAccountRequest request) {
        User user = currentUser();
        validateAccountDeletion(request);
        userDeletionService.deletePermanently(user);
        return new AuthResponses.MessageResponse("OK");
    }

    @Override
    @Transactional
    public UserResponses.ExportDataResponse exportCurrentUserData() {
        User user = currentUser();
        protectOnboardingProfile(user);
        List<DomainResponses.PanicAlertResponse> panicAlerts = panicAlertRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged())
            .getContent()
            .stream()
            .map(alert -> toPanicResponse(alert, user))
            .toList();
        return new UserResponses.ExportDataResponse(
            Instant.now(),
            userMapper.toResponse(user),
            userSettingsRepository.findByUser(user).map(userSettingsMapper::toResponse).orElse(null),
            diaryEntryRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged()).map(entry -> diaryResponse(entry, user)).getContent(),
            moodLogRepository.findByUserAndLoggedAtBetween(user, Instant.EPOCH, Instant.now().plusSeconds(315360000), org.springframework.data.domain.Pageable.unpaged()).map(log -> moodResponse(log, user)).getContent(),
            chatSessionRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged())
                .map(session -> chatResponse(session, user))
                .getContent(),
            contactRepository.findByUserOrderByPriorityAscNameAsc(user).stream().map(contact -> contactResponse(contact, user)).toList(),
            panicAlerts,
            Map.of("format", "aura-export-v1")
        );
    }

    @Override
    @Transactional
    public byte[] exportCurrentUserDataPdf() {
        return userExportPdfService.render(exportCurrentUserData());
    }

    private void validateAccountDeletion(UserRequests.DeleteAccountRequest request) {
        if (request == null || !"ELIMINAR MI CUENTA".equals(request.confirmationText())) {
            throw new BusinessException("error.account_delete_confirmation_required");
        }
    }

    private DomainResponses.PanicAlertResponse toPanicResponse(PanicAlert alert, User user) {
        String notes = contentCryptoService.decrypt(user.getId(), "panic.notes", alert.getNotes());
        Map<String, Object> contextJson = alert.getContextJson() == null
            ? new LinkedHashMap<>()
            : contentCryptoService.decryptJsonMap(user.getId(), "panic.context", alert.getContextJson());
        if (contentCryptoService.isEnabled()) {
            boolean notesNeedProtection = alert.getNotes() != null && !contentCryptoService.isEncrypted(alert.getNotes());
            boolean contextNeedsProtection = hasLegacyJsonStrings(alert.getContextJson());
            if (notesNeedProtection || contextNeedsProtection) {
                if (notesNeedProtection) {
                    alert.setNotes(contentCryptoService.encrypt(user.getId(), "panic.notes", notes));
                }
                if (contextNeedsProtection) {
                    alert.setContextJson(contentCryptoService.encryptJsonMap(user.getId(), "panic.context", contextJson));
                }
                panicAlertRepository.save(alert);
            }
        }
        List<DomainResponses.PanicNotificationResponse> notifications = notificationResultRepository.findByAlert(alert)
            .stream()
            .map(result -> toNotificationResponse(result, user))
            .toList();
        return new DomainResponses.PanicAlertResponse(
            alert.getId(),
            alert.getTriggeredAt(),
            alert.getResolvedAt(),
            notes,
            contextJson,
            notifications,
            alert.getCreatedAt(),
            alert.getUpdatedAt()
        );
    }

    private DomainResponses.PanicNotificationResponse toNotificationResponse(PanicNotificationResult result, User user) {
        String details = contentCryptoService.decrypt(user.getId(), "panic.notification-details", result.getDetails());
        if (contentCryptoService.isEnabled()) {
            boolean detailsNeedProtection = result.getDetails() != null && !contentCryptoService.isEncrypted(result.getDetails());
            if (detailsNeedProtection) {
                result.setDetails(contentCryptoService.encrypt(user.getId(), "panic.notification-details", details));
                notificationResultRepository.save(result);
            }
        }
        return new DomainResponses.PanicNotificationResponse(
            result.getId(),
            result.getContact() == null ? null : result.getContact().getId(),
            result.getContact() == null ? null : contentCryptoService.decrypt(user.getId(), "contact.name", result.getContact().getName()),
            result.getChannel(),
            result.getStatus(),
            details,
            result.getCreatedAt()
        );
    }

    private void createAndSendVerificationToken(User user) {
        String raw = TokenHashing.newOpaqueToken();
        verificationTokenRepository.save(EmailVerificationToken.builder()
            .user(user)
            .tokenHash(TokenHashing.sha256(raw))
            .expiresAt(Instant.now().plus(properties.getEmail().getVerificationTokenTtlHours(), ChronoUnit.HOURS))
            .build());
        verificationEmailService.sendVerificationEmail(user, raw);
    }

    private void validateOnboarding(UserRequests.CompleteOnboardingRequest request) {
        if (!Boolean.TRUE.equals(request.privacyAccepted())
            || !Boolean.TRUE.equals(request.termsAccepted())
            || !Boolean.TRUE.equals(request.supportOnlyAccepted())
            || !Boolean.TRUE.equals(request.ageConfirmed())) {
            throw new BusinessException("error.onboarding_consent_required");
        }
        if (!"es".equals(request.language().trim().toLowerCase(Locale.ROOT))) {
            throw new BusinessException("error.unsupported_language");
        }
        validateTrustedContact(request.trustedContact());
    }

    private void validateTrustedContact(UserRequests.TrustedContactRequest contact) {
        if (contact == null) {
            return;
        }
        boolean hasName = hasText(contact.name());
        boolean hasPhone = hasText(contact.phone());
        boolean hasRelationship = hasText(contact.relationship());
        if ((hasName || hasPhone || hasRelationship) && (!hasName || !hasPhone)) {
            throw new BusinessException("error.onboarding_contact_partial");
        }
    }

    private Map<String, Object> onboardingProfile(UserRequests.CompleteOnboardingRequest request) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("goals", cleanList(request.goals()));
        profile.put("anxietyTriggers", cleanList(request.anxietyTriggers()));
        profile.put("currentMood", currentMood(request.currentMood()));
        profile.put("toolPreferences", cleanList(request.toolPreferences()));
        profile.put("notifications", request.notifications() == null ? Map.of() : request.notifications());
        return profile;
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(this::hasText)
            .map(String::trim)
            .toList();
    }

    private Map<String, Object> currentMood(UserRequests.CurrentMoodRequest mood) {
        if (mood == null) {
            return Map.of();
        }
        return Map.of(
            "label", mood.label().trim(),
            "intensity", mood.intensity()
        );
    }

    private void updateSettings(User user, String language, String timezone, Map<String, Object> notifications) {
        UserSettings settings = userSettingsRepository.findByUser(user).orElseGet(() -> UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language(language)
            .timezone(timezone)
            .build());
        settings.setLanguage(language);
        settings.setTimezone(timezone);
        if (notifications != null) {
            settings.setNotificationPreferences(notifications);
        }
        userSettingsRepository.save(settings);
    }

    private void createTrustedContactIfPresent(User user, UserRequests.TrustedContactRequest contact) {
        if (contact == null || !hasText(contact.name()) && !hasText(contact.phone()) && !hasText(contact.relationship())) {
            return;
        }
        contactRepository.save(Contact.builder()
            .user(user)
            .name(contentCryptoService.encrypt(user.getId(), "contact.name", contact.name().trim()))
            .phone(contentCryptoService.encrypt(user.getId(), "contact.phone", contact.phone().trim()))
            .relationship(contentCryptoService.encrypt(user.getId(), "contact.relationship", blankToNull(contact.relationship())))
            .priority(1)
            .available(true)
            .sosEnabled(true)
            .build());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void protectOnboardingProfile(User user) {
        Map<String, Object> profile = user.getOnboardingProfile() == null
            ? new LinkedHashMap<>()
            : contentCryptoService.decryptJsonMap(user.getId(), "user.onboarding-profile", user.getOnboardingProfile());
        if (contentCryptoService.isEnabled() && hasLegacyJsonStrings(user.getOnboardingProfile())) {
            user.setOnboardingProfile(contentCryptoService.encryptJsonMap(user.getId(), "user.onboarding-profile", profile));
            userRepository.save(user);
        }
    }

    private DomainResponses.DiaryEntryResponse diaryResponse(com.auraia.backend.models.entities.DiaryEntry entry, User user) {
        String title = contentCryptoService.decrypt(user.getId(), "diary.title", entry.getTitle());
        String content = contentCryptoService.decrypt(user.getId(), "diary.content", entry.getContent());
        String moodLabel = contentCryptoService.decrypt(user.getId(), "diary.mood-label", entry.getMoodLabel());
        if (contentCryptoService.isEnabled()) {
            boolean changed = false;
            String encryptedTitle = encryptLegacyValue(user.getId(), "diary.title", entry.getTitle(), title);
            String encryptedContent = encryptLegacyValue(user.getId(), "diary.content", entry.getContent(), content);
            String encryptedMoodLabel = encryptLegacyValue(user.getId(), "diary.mood-label", entry.getMoodLabel(), moodLabel);
            List<String> tokens = contentCryptoService.searchTokens(user.getId(), title, content);
            if (!java.util.Objects.equals(entry.getTitle(), encryptedTitle)) {
                entry.setTitle(encryptedTitle);
                changed = true;
            }
            if (!java.util.Objects.equals(entry.getContent(), encryptedContent)) {
                entry.setContent(encryptedContent);
                changed = true;
            }
            if (!java.util.Objects.equals(entry.getMoodLabel(), encryptedMoodLabel)) {
                entry.setMoodLabel(encryptedMoodLabel);
                changed = true;
            }
            if (!java.util.Objects.equals(entry.getSearchTokens() == null ? List.of() : entry.getSearchTokens(), tokens)) {
                entry.setSearchTokens(tokens);
                changed = true;
            }
            if (changed) {
                diaryEntryRepository.save(entry);
            }
        }
        return new DomainResponses.DiaryEntryResponse(
            entry.getId(),
            title,
            content,
            entry.getMoodScore(),
            moodLabel,
            entry.getTags() == null ? List.of() : List.copyOf(entry.getTags()),
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }

    private DomainResponses.MoodLogResponse moodResponse(com.auraia.backend.models.entities.MoodLog log, User user) {
        String note = contentCryptoService.decrypt(user.getId(), "mood.note", log.getNote());
        if (contentCryptoService.isEnabled() && log.getNote() != null && !contentCryptoService.isEncrypted(log.getNote())) {
            log.setNote(contentCryptoService.encrypt(user.getId(), "mood.note", note));
            moodLogRepository.save(log);
        }
        return new DomainResponses.MoodLogResponse(
            log.getId(),
            log.getBeforeLevel(),
            log.getAfterLevel(),
            note,
            log.getLoggedAt(),
            log.getCreatedAt(),
            log.getUpdatedAt()
        );
    }

    private DomainResponses.ChatSessionResponse chatResponse(com.auraia.backend.models.entities.ChatSession session, User user) {
        String title = contentCryptoService.decrypt(user.getId(), "chat.title", session.getTitle());
        List<Map<String, Object>> messages = session.getMessages() == null ? List.of() : session.getMessages().stream()
            .map(message -> {
                Map<String, Object> copy = new LinkedHashMap<>(message);
                Object content = copy.get("content");
                if (content instanceof String text) {
                    copy.put("content", contentCryptoService.decrypt(user.getId(), "chat.message-content", text));
                }
                return copy;
            })
            .toList();
        if (contentCryptoService.isEnabled()) {
            boolean titleNeedsProtection = session.getTitle() != null && !contentCryptoService.isEncrypted(session.getTitle());
            boolean messagesNeedProtection = hasLegacyMessageContent(session.getMessages());
            if (titleNeedsProtection || messagesNeedProtection) {
                if (titleNeedsProtection) {
                    session.setTitle(contentCryptoService.encrypt(user.getId(), "chat.title", title));
                }
                if (messagesNeedProtection) {
                    session.setMessages(messages.stream()
                        .map(message -> {
                            Map<String, Object> copy = new LinkedHashMap<>(message);
                            Object content = copy.get("content");
                            if (content instanceof String text) {
                                copy.put("content", contentCryptoService.encrypt(user.getId(), "chat.message-content", text));
                            }
                            return copy;
                        })
                        .toList());
                }
                chatSessionRepository.save(session);
            }
        }
        return new DomainResponses.ChatSessionResponse(session.getId(), title, messages, session.getStartedAt(), session.getUpdatedAt());
    }

    private DomainResponses.ContactResponse contactResponse(Contact contact, User user) {
        String name = contentCryptoService.decrypt(user.getId(), "contact.name", contact.getName());
        String phone = contentCryptoService.decrypt(user.getId(), "contact.phone", contact.getPhone());
        String relationship = contentCryptoService.decrypt(user.getId(), "contact.relationship", contact.getRelationship());
        if (contentCryptoService.isEnabled()) {
            boolean changed = false;
            String encryptedName = encryptLegacyValue(user.getId(), "contact.name", contact.getName(), name);
            String encryptedPhone = encryptLegacyValue(user.getId(), "contact.phone", contact.getPhone(), phone);
            String encryptedRelationship = encryptLegacyValue(user.getId(), "contact.relationship", contact.getRelationship(), relationship);
            if (!java.util.Objects.equals(contact.getName(), encryptedName)) {
                contact.setName(encryptedName);
                changed = true;
            }
            if (!java.util.Objects.equals(contact.getPhone(), encryptedPhone)) {
                contact.setPhone(encryptedPhone);
                changed = true;
            }
            if (!java.util.Objects.equals(contact.getRelationship(), encryptedRelationship)) {
                contact.setRelationship(encryptedRelationship);
                changed = true;
            }
            if (changed) {
                contactRepository.save(contact);
            }
        }
        return new DomainResponses.ContactResponse(
            contact.getId(),
            name,
            phone,
            relationship,
            contact.getPriority(),
            contact.isAvailable(),
            contact.isSosEnabled(),
            contact.getCreatedAt(),
            contact.getUpdatedAt()
        );
    }

    private String encryptLegacyValue(java.util.UUID userId, String scope, String storedValue, String plainText) {
        return storedValue != null && !contentCryptoService.isEncrypted(storedValue)
            ? contentCryptoService.encrypt(userId, scope, plainText)
            : storedValue;
    }

    private boolean hasLegacyJsonStrings(Object value) {
        if (value instanceof String text) {
            return !contentCryptoService.isEncrypted(text);
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(this::hasLegacyJsonStrings);
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(this::hasLegacyJsonStrings);
        }
        return false;
    }

    private boolean hasLegacyMessageContent(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (Map<String, Object> message : messages) {
            Object content = message.get("content");
            if (content instanceof String text && !contentCryptoService.isEncrypted(text)) {
                return true;
            }
        }
        return false;
    }
}
