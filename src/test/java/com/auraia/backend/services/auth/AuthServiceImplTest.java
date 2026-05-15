package com.auraia.backend.services.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.repositories.EmailVerificationTokenRepository;
import com.auraia.backend.repositories.PasswordResetTokenRepository;
import com.auraia.backend.repositories.RefreshTokenRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    EmailVerificationTokenRepository verificationTokenRepository;
    @Mock
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    VerificationEmailService verificationEmailService;
    @Mock
    PasswordResetEmailService passwordResetEmailService;
    @Mock
    WelcomeEmailService welcomeEmailService;
    @Mock
    TurnstileService turnstileService;
    @Mock
    PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserMapper userMapper;
    @Mock
    AuthTokenService authTokenService;

    AuthServiceImpl service;
    AppProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getEmail().setVerificationTokenTtlHours(24);

        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("auth.email.sent", Locale.ENGLISH, "Verification email sent.");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new AuthServiceImpl(
            userRepository,
            userSettingsRepository,
            refreshTokenRepository,
            verificationTokenRepository,
            passwordResetTokenRepository,
            verificationEmailService,
            passwordResetEmailService,
            welcomeEmailService,
            turnstileService,
            passwordPolicyValidator,
            passwordEncoder,
            userMapper,
            authTokenService,
            properties,
            messageSource
        );
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void registerRejectsEmailWhenExistingAccountIsNotVerified() {
        User user = User.builder()
            .email("pending@example.com")
            .passwordHash("hash")
            .name("Pending User")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(false)
            .build();

        when(turnstileService.verify(null, null)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.register(new AuthRequests.RegisterRequest(
            "Pending User",
            "pending@example.com",
            "StrongPassword123!",
            null
        ))).isInstanceOf(BusinessException.class);

        verify(verificationTokenRepository, never()).save(any());
        verify(verificationEmailService, never()).sendVerificationEmail(any(User.class), any(String.class));
        verify(passwordPolicyValidator, never()).validate(any(String.class));
    }

    @Test
    void registerRejectsEmailWhenExistingAccountIsVerified() {
        User user = User.builder()
            .email("verified@example.com")
            .passwordHash("hash")
            .name("Verified User")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();

        when(turnstileService.verify(null, null)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("verified@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.register(new AuthRequests.RegisterRequest(
            "Verified User",
            "verified@example.com",
            "StrongPassword123!",
            null
        ))).isInstanceOf(BusinessException.class);

        verify(verificationEmailService, never()).sendVerificationEmail(any(User.class), any(String.class));
    }
}
