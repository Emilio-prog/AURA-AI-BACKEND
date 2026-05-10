package com.auraia.backend.services.user;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.ContactMapper;
import com.auraia.backend.mappers.DiaryEntryMapper;
import com.auraia.backend.mappers.MoodLogMapper;
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
    private final DiaryEntryMapper diaryEntryMapper;
    private final MoodLogMapper moodLogMapper;
    private final ContactMapper contactMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final UserDeletionService userDeletionService;
    private final VerificationEmailService verificationEmailService;
    private final AppProperties properties;

    @Override
    @Transactional(readOnly = true)
    public UserResponses.UserResponse getCurrentProfile() {
        return userMapper.toResponse(currentUser());
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
        user.setOnboardingProfile(onboardingProfile(request));

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
    public AuthResponses.MessageResponse deleteCurrentAccount() {
        userDeletionService.anonymizeAndSoftDelete(currentUser());
        return new AuthResponses.MessageResponse("OK");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponses.ExportDataResponse exportCurrentUserData() {
        User user = currentUser();
        List<DomainResponses.PanicAlertResponse> panicAlerts = panicAlertRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged())
            .getContent()
            .stream()
            .map(this::toPanicResponse)
            .toList();
        return new UserResponses.ExportDataResponse(
            Instant.now(),
            userMapper.toResponse(user),
            userSettingsRepository.findByUser(user).map(userSettingsMapper::toResponse).orElse(null),
            diaryEntryRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged()).map(diaryEntryMapper::toResponse).getContent(),
            moodLogRepository.findByUserAndLoggedAtBetween(user, Instant.EPOCH, Instant.now().plusSeconds(315360000), org.springframework.data.domain.Pageable.unpaged()).map(moodLogMapper::toResponse).getContent(),
            chatSessionRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged())
                .map(session -> new DomainResponses.ChatSessionResponse(session.getId(), session.getTitle(), session.getMessages(), session.getStartedAt(), session.getUpdatedAt()))
                .getContent(),
            contactRepository.findByUserOrderByPriorityAscNameAsc(user).stream().map(contactMapper::toResponse).toList(),
            panicAlerts,
            Map.of("format", "aura-export-v1")
        );
    }

    private DomainResponses.PanicAlertResponse toPanicResponse(PanicAlert alert) {
        List<DomainResponses.PanicNotificationResponse> notifications = notificationResultRepository.findByAlert(alert)
            .stream()
            .map(this::toNotificationResponse)
            .toList();
        return new DomainResponses.PanicAlertResponse(
            alert.getId(),
            alert.getTriggeredAt(),
            alert.getResolvedAt(),
            alert.getNotes(),
            alert.getContextJson(),
            notifications,
            alert.getCreatedAt(),
            alert.getUpdatedAt()
        );
    }

    private DomainResponses.PanicNotificationResponse toNotificationResponse(PanicNotificationResult result) {
        return new DomainResponses.PanicNotificationResponse(
            result.getId(),
            result.getContact() == null ? null : result.getContact().getId(),
            result.getContact() == null ? null : result.getContact().getName(),
            result.getChannel(),
            result.getStatus(),
            result.getDetails(),
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
            .name(contact.name().trim())
            .phone(contact.phone().trim())
            .relationship(blankToNull(contact.relationship()))
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
}
