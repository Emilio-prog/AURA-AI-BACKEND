package com.auraia.backend.services.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.mappers.ContactMapper;
import com.auraia.backend.mappers.DiaryEntryMapper;
import com.auraia.backend.mappers.MoodLogMapper;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.mappers.UserSettingsMapper;
import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
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
import com.auraia.backend.security.UserPrincipal;
import com.auraia.backend.services.UserDeletionService;
import com.auraia.backend.services.auth.PasswordPolicyValidator;
import com.auraia.backend.services.auth.VerificationEmailService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    DiaryEntryRepository diaryEntryRepository;
    @Mock
    MoodLogRepository moodLogRepository;
    @Mock
    ChatSessionRepository chatSessionRepository;
    @Mock
    ContactRepository contactRepository;
    @Mock
    PanicAlertRepository panicAlertRepository;
    @Mock
    PanicNotificationResultRepository notificationResultRepository;
    @Mock
    EmailVerificationTokenRepository verificationTokenRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    UserSettingsMapper userSettingsMapper;
    @Mock
    DiaryEntryMapper diaryEntryMapper;
    @Mock
    MoodLogMapper moodLogMapper;
    @Mock
    ContactMapper contactMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    UserDeletionService userDeletionService;
    @Mock
    VerificationEmailService verificationEmailService;

    UserServiceImpl service;
    UUID userId;
    User user;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
            userRepository,
            userSettingsRepository,
            diaryEntryRepository,
            moodLogRepository,
            chatSessionRepository,
            contactRepository,
            panicAlertRepository,
            notificationResultRepository,
            verificationTokenRepository,
            userMapper,
            userSettingsMapper,
            diaryEntryMapper,
            moodLogMapper,
            contactMapper,
            passwordEncoder,
            passwordPolicyValidator,
            userDeletionService,
            verificationEmailService,
            null
        );
        userId = UUID.randomUUID();
        user = User.builder()
            .email("emilio@example.com")
            .passwordHash("hash")
            .name("Emilio Old")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        lenient().when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User mapped = invocation.getArgument(0);
            return new UserResponses.UserResponse(
                mapped.getId(),
                mapped.getName(),
                mapped.getEmail(),
                mapped.getRole(),
                mapped.getPlan(),
                mapped.isEmailVerified(),
                mapped.getCreatedAt(),
                mapped.getOnboardedAt()
            );
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeOnboardingStoresProfileSettingsAndSosContact() {
        UserSettings settings = UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .build();
        when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponses.UserResponse response = service.completeOnboarding(validRequest());

        assertThat(response.onboardedAt()).isNotNull();
        assertThat(user.getName()).isEqualTo("Emilio");
        assertThat(user.getOnboardingConsentAt()).isNotNull();
        assertThat(user.getOnboardingConsentVersion()).isEqualTo("privacy:2026-05-10;terms:2026-05-10;onboarding:v1");
        assertThat(user.getOnboardingProfile())
            .containsEntry("goals", List.of("sleep", "anxiety", "routine"))
            .containsEntry("toolPreferences", List.of("breathing", "journal", "chat"));
        assertThat(user.getOnboardingProfile().get("currentMood"))
            .isEqualTo(Map.of("label", "ansiedad", "intensity", 6));
        assertThat(settings.getLanguage()).isEqualTo("es");
        assertThat(settings.getTimezone()).isEqualTo("Europe/Madrid");
        assertThat(settings.getNotificationPreferences())
            .containsEntry("enabled", true)
            .containsEntry("dailyReminderTime", "20:00");

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact contact = contactCaptor.getValue();
        assertThat(contact.getUser()).isSameAs(user);
        assertThat(contact.getName()).isEqualTo("Ana");
        assertThat(contact.getPhone()).isEqualTo("+34 600 000 000");
        assertThat(contact.getRelationship()).isEqualTo("Hermana");
        assertThat(contact.isAvailable()).isTrue();
        assertThat(contact.isSosEnabled()).isTrue();
        assertThat(contact.getPriority()).isEqualTo(1);
    }

    @Test
    void completeOnboardingRejectsMissingConsent() {
        UserRequests.CompleteOnboardingRequest request = new UserRequests.CompleteOnboardingRequest(
            "Emilio",
            "es",
            "Europe/Madrid",
            true,
            false,
            true,
            true,
            List.of(),
            List.of(),
            null,
            List.of(),
            Map.of(),
            null
        );

        assertThatThrownBy(() -> service.completeOnboarding(request))
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.onboarding_consent_required");

        verify(userRepository, never()).save(any(User.class));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void completeOnboardingIsIdempotentWhenAlreadyOnboarded() {
        user.setOnboardedAt(Instant.parse("2026-05-10T12:00:00Z"));

        UserResponses.UserResponse response = service.completeOnboarding(validRequest());

        assertThat(response.onboardedAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
        verify(userRepository, never()).save(any(User.class));
        verify(userSettingsRepository, never()).save(any(UserSettings.class));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    private UserRequests.CompleteOnboardingRequest validRequest() {
        return new UserRequests.CompleteOnboardingRequest(
            " Emilio ",
            "es",
            "Europe/Madrid",
            true,
            true,
            true,
            true,
            List.of("sleep", "anxiety", "routine"),
            List.of("work", "social", "night"),
            new UserRequests.CurrentMoodRequest("ansiedad", 6),
            List.of("breathing", "journal", "chat"),
            Map.of("enabled", true, "dailyReminderTime", "20:00"),
            new UserRequests.TrustedContactRequest("Ana", "+34 600 000 000", "Hermana")
        );
    }
}
