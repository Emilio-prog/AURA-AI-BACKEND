package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.UnauthorizedException;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.EmailVerificationToken;
import com.auraia.backend.models.entities.PasswordResetToken;
import com.auraia.backend.models.entities.RefreshToken;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.EmailVerificationTokenRepository;
import com.auraia.backend.repositories.PasswordResetTokenRepository;
import com.auraia.backend.repositories.RefreshTokenRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.utils.TokenHashing;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final VerificationEmailService verificationEmailService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final WelcomeEmailService welcomeEmailService;
    private final TurnstileService turnstileService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthTokenService authTokenService;
    private final AppProperties properties;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public AuthResponses.PendingVerificationResponse register(AuthRequests.RegisterRequest request) {
        if (!turnstileService.verify(request.captchaToken(), null)) {
            throw new BusinessException("error.captcha_invalid");
        }
        String email = normalizeEmail(request.email());
        var existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            throw new BusinessException("error.email_in_use");
        }
        passwordPolicyValidator.validate(request.password());

        boolean admin = isConfiguredAdmin(email);
        boolean emailVerified = admin || shouldAutoVerifyEmail();
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name().trim())
            .role(admin ? Role.ADMIN : Role.USER)
            .plan(Plan.FREE)
            .emailVerified(emailVerified)
            .build();
        userRepository.save(user);
        createDefaultSettings(user);

        if (!emailVerified) {
            createAndSendVerificationToken(user);
        }

        String messageCode = emailVerified ? "auth.register.verified" : "auth.register.pending";
        return new AuthResponses.PendingVerificationResponse(email, message(messageCode), !emailVerified);
    }

    @Override
    @Transactional
    public AuthResponses.AuthResponse login(AuthRequests.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email()))
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException("error.email_not_verified");
        }
        return authTokenService.issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponses.AuthResponse refresh(AuthRequests.RefreshTokenRequest request) {
        String hash = TokenHashing.sha256(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new BusinessException("error.invalid_token"));
        Instant now = Instant.now();
        if (!refreshToken.isUsable(now) || !refreshToken.getUser().isEmailVerified()) {
            throw new BusinessException("error.invalid_token");
        }
        refreshToken.setRevokedAt(now);
        refreshTokenRepository.save(refreshToken);
        return authTokenService.issueTokens(refreshToken.getUser());
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse logout(AuthRequests.LogoutRequest request) {
        String hash = TokenHashing.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
        return new AuthResponses.MessageResponse("OK");
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByTokenHash(TokenHashing.sha256(token))
            .orElseThrow(() -> new BusinessException("error.invalid_token"));
        if (!verificationToken.isUsable(Instant.now())) {
            throw new BusinessException("error.invalid_token");
        }
        verificationToken.setConsumedAt(Instant.now());
        User verifiedUser = verificationToken.getUser();
        verifiedUser.setEmailVerified(true);
        verificationTokenRepository.save(verificationToken);
        userRepository.save(verifiedUser);
        welcomeEmailService.sendWelcomeEmail(verifiedUser);
        return new AuthResponses.MessageResponse(message("auth.email.verified"));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse resendVerification(AuthRequests.ResendVerificationRequest request) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email()))
            .filter(user -> !user.isEmailVerified())
            .ifPresent(this::createAndSendVerificationToken);
        return new AuthResponses.MessageResponse(message("auth.email.sent"));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse forgotPassword(AuthRequests.ForgotPasswordRequest request) {
        if (!turnstileService.verify(request.captchaToken(), null)) {
            throw new BusinessException("error.captcha_invalid");
        }
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email()))
            .ifPresent(this::createAndSendPasswordResetToken);
        return new AuthResponses.MessageResponse(message("auth.password_reset.requested"));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse resetPassword(AuthRequests.ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHashing.sha256(request.token()))
            .orElseThrow(() -> new BusinessException("error.invalid_token"));
        Instant now = Instant.now();
        if (!token.isUsable(now)) {
            throw new BusinessException("error.invalid_token");
        }
        passwordPolicyValidator.validate(request.password());

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        token.setConsumedAt(now);
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.invalidateAllActiveByUser(user, now);

        refreshTokenRepository.deleteAllByUser(user);

        return new AuthResponses.MessageResponse(message("auth.password_reset.success"));
    }

    private void createAndSendPasswordResetToken(User user) {
        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateAllActiveByUser(user, now);
        String raw = TokenHashing.newOpaqueToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
            .user(user)
            .tokenHash(TokenHashing.sha256(raw))
            .expiresAt(now.plus(properties.getEmail().getPasswordResetTokenTtlMinutes(), ChronoUnit.MINUTES))
            .build());
        passwordResetEmailService.sendResetEmail(user, raw);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponses.UserResponse me() {
        User user = userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return userMapper.toResponse(user);
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

    private void createDefaultSettings(User user) {
        userSettingsRepository.save(UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .build());
    }

    private boolean isConfiguredAdmin(String email) {
        return properties.getAdminEmails().stream()
            .map(this::normalizeEmail)
            .anyMatch(email::equals);
    }

    private boolean shouldAutoVerifyEmail() {
        return !properties.getEmail().isEnabled() && properties.getEmail().isAutoVerifyWhenDisabled();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String message(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
