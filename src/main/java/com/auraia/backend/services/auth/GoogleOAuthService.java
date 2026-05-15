package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.UnauthorizedException;
import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.entities.OAuthExchangeCode;
import com.auraia.backend.models.entities.OAuthIdentity;
import com.auraia.backend.models.entities.OAuthState;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.OAuthExchangeCodeRepository;
import com.auraia.backend.repositories.OAuthIdentityRepository;
import com.auraia.backend.repositories.OAuthStateRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.utils.TokenHashing;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String FLOW_LOGIN = "LOGIN";
    private static final String FLOW_LINK = "LINK";
    private static final String LOCALHOST_FRONTEND_BASE_URL = "http://localhost:5173";
    private static final String LOOPBACK_FRONTEND_BASE_URL = "http://127.0.0.1:5173";

    private final AppProperties properties;
    private final GoogleOAuthClient googleOAuthClient;
    private final OAuthStateRepository stateRepository;
    private final OAuthIdentityRepository identityRepository;
    private final OAuthExchangeCodeRepository exchangeCodeRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final AuthTokenService authTokenService;

    @Transactional
    public AuthResponses.OAuthStartResponse startLogin() {
        return startLogin(null);
    }

    @Transactional
    public AuthResponses.OAuthStartResponse startLogin(String frontendBaseUrl) {
        ensureEnabled();
        String state = createState(null, FLOW_LOGIN, frontendBaseUrl);
        return new AuthResponses.OAuthStartResponse(googleOAuthClient.authorizationUrl(state));
    }

    @Transactional
    public AuthResponses.OAuthStartResponse startLink() {
        return startLink(null);
    }

    @Transactional
    public AuthResponses.OAuthStartResponse startLink(String frontendBaseUrl) {
        ensureEnabled();
        User user = currentUser();
        String state = createState(user, FLOW_LINK, frontendBaseUrl);
        return new AuthResponses.OAuthStartResponse(googleOAuthClient.authorizationUrl(state));
    }

    @Transactional
    public String handleCallback(String state, String code, String providerError) {
        String callbackBaseUrl = callbackBaseUrlFromState(state);
        if (providerError != null && !providerError.isBlank()) {
            return frontendCallbackUrl(callbackBaseUrl, "error", "Google rechazo la autorizacion.");
        }
        try {
            if (state == null || state.isBlank() || code == null || code.isBlank()) {
                throw new BusinessException("error.google_oauth_failed");
            }
            OAuthState oauthState = stateRepository.findByStateHash(TokenHashing.sha256(state))
                .orElseThrow(() -> new BusinessException("error.invalid_token"));
            Instant now = Instant.now();
            if (!oauthState.isUsable(now)) {
                throw new BusinessException("error.invalid_token");
            }

            GoogleOAuthUser googleUser = googleOAuthClient.fetchUser(code);
            if (!googleUser.emailVerified()) {
                throw new BusinessException("error.google_oauth_email_unverified");
            }

            User user = resolveUser(oauthState, googleUser);
            linkIdentity(user, googleUser);
            oauthState.setConsumedAt(now);
            stateRepository.save(oauthState);

            String exchangeCode = createExchangeCode(user, now);
            return frontendCallbackUrl(callbackBaseUrl, "code", exchangeCode);
        } catch (BusinessException ex) {
            log.warn("Google OAuth callback rejected: {}", ex.getMessage());
            return frontendCallbackUrl(callbackBaseUrl, "error", "No se pudo iniciar sesion con Google.");
        } catch (Exception ex) {
            log.warn("Google OAuth callback failed", ex);
            return frontendCallbackUrl(callbackBaseUrl, "error", "No se pudo iniciar sesion con Google.");
        }
    }

    @Transactional
    public AuthResponses.AuthResponse exchange(AuthRequests.OAuthExchangeRequest request) {
        OAuthExchangeCode exchangeCode = exchangeCodeRepository.findByCodeHash(TokenHashing.sha256(request.code()))
            .orElseThrow(() -> new BusinessException("error.invalid_token"));
        Instant now = Instant.now();
        if (!exchangeCode.isUsable(now)) {
            throw new BusinessException("error.invalid_token");
        }
        exchangeCode.setConsumedAt(now);
        exchangeCodeRepository.save(exchangeCode);
        return authTokenService.issueTokens(exchangeCode.getUser());
    }

    @Transactional(readOnly = true)
    public AuthResponses.OAuthStatusResponse status() {
        User user = currentUser();
        return identityRepository.findByUserAndProviderAndActiveTrue(user, PROVIDER_GOOGLE)
            .map(identity -> new AuthResponses.OAuthStatusResponse(true, identity.getEmail(), identity.getLinkedAt()))
            .orElseGet(() -> new AuthResponses.OAuthStatusResponse(false, null, null));
    }

    @Transactional
    public AuthResponses.MessageResponse unlink() {
        User user = currentUser();
        OAuthIdentity identity = identityRepository.findByUserAndProviderAndActiveTrue(user, PROVIDER_GOOGLE)
            .orElse(null);
        if (identity == null) {
            return new AuthResponses.MessageResponse("OK");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("error.google_oauth_password_required");
        }
        identity.setActive(false);
        identity.setRevokedAt(Instant.now());
        identityRepository.save(identity);
        return new AuthResponses.MessageResponse("OK");
    }

    private String createState(User user, String flow, String frontendBaseUrl) {
        String rawState = TokenHashing.newOpaqueToken() + "." + encodeFrontendBaseUrl(frontendBaseUrl);
        OAuthState state = OAuthState.builder()
            .user(user)
            .stateHash(TokenHashing.sha256(rawState))
            .flow(flow)
            .expiresAt(Instant.now().plus(properties.getGoogleOAuth().getStateTtlMinutes(), ChronoUnit.MINUTES))
            .build();
        stateRepository.save(state);
        return rawState;
    }

    private User resolveUser(OAuthState state, GoogleOAuthUser googleUser) {
        if (FLOW_LINK.equals(state.getFlow())) {
            if (state.getUser() == null || state.getUser().isDeleted()) {
                throw new UnauthorizedException("Authentication required");
            }
            String email = normalizeEmail(googleUser.email());
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .filter(existing -> !existing.getId().equals(state.getUser().getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("error.google_oauth_email_conflict");
                });
            return state.getUser();
        }

        return identityRepository.findByProviderAndProviderSubjectAndActiveTrue(PROVIDER_GOOGLE, googleUser.subject())
            .map(OAuthIdentity::getUser)
            .filter(user -> !user.isDeleted())
            .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizeEmail(googleUser.email()))
                .map(user -> {
                    if (user.isDeleted()) {
                        throw new BusinessException("error.google_oauth_email_conflict");
                    }
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                })
                .orElseGet(() -> createGoogleUser(googleUser)));
    }

    private void linkIdentity(User user, GoogleOAuthUser googleUser) {
        identityRepository.findByProviderAndProviderSubjectAndActiveTrue(PROVIDER_GOOGLE, googleUser.subject())
            .filter(existing -> !existing.getUser().getId().equals(user.getId()))
            .ifPresent(existing -> {
                throw new BusinessException("error.google_oauth_email_conflict");
            });

        OAuthIdentity identity = identityRepository.findByUserAndProviderAndActiveTrue(user, PROVIDER_GOOGLE)
            .orElse(null);
        if (identity != null && !identity.getProviderSubject().equals(googleUser.subject())) {
            throw new BusinessException("error.google_oauth_email_conflict");
        }
        Instant now = Instant.now();
        if (identity == null) {
            identity = OAuthIdentity.builder()
                .user(user)
                .provider(PROVIDER_GOOGLE)
                .providerSubject(googleUser.subject())
                .active(true)
                .linkedAt(now)
                .build();
        }
        identity.setEmail(normalizeEmail(googleUser.email()));
        identity.setEmailVerified(googleUser.emailVerified());
        identityRepository.save(identity);
    }

    private User createGoogleUser(GoogleOAuthUser googleUser) {
        String email = normalizeEmail(googleUser.email());
        User user = User.builder()
            .email(email)
            .passwordHash(null)
            .name(displayName(googleUser, email))
            .role(isConfiguredAdmin(email) ? Role.ADMIN : Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        userRepository.save(user);
        userSettingsRepository.save(UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .build());
        return user;
    }

    private String createExchangeCode(User user, Instant now) {
        String rawCode = TokenHashing.newOpaqueToken();
        exchangeCodeRepository.save(OAuthExchangeCode.builder()
            .user(user)
            .codeHash(TokenHashing.sha256(rawCode))
            .expiresAt(now.plus(properties.getGoogleOAuth().getExchangeCodeTtlMinutes(), ChronoUnit.MINUTES))
            .build());
        return rawCode;
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    private String frontendCallbackUrl(String base, String paramName, String value) {
        String separator = base.endsWith("/") ? "" : "/";
        return base + separator + "#/auth/google/callback?" + paramName + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String callbackBaseUrlFromState(String state) {
        if (state == null || state.isBlank()) {
            return normalizeFrontendBaseUrl(properties.getFrontendBaseUrl());
        }
        int separator = state.lastIndexOf('.');
        if (separator < 0 || separator == state.length() - 1) {
            return normalizeFrontendBaseUrl(properties.getFrontendBaseUrl());
        }
        try {
            String encoded = state.substring(separator + 1);
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            return normalizeFrontendBaseUrl(decoded);
        } catch (IllegalArgumentException ex) {
            return normalizeFrontendBaseUrl(properties.getFrontendBaseUrl());
        }
    }

    private String encodeFrontendBaseUrl(String frontendBaseUrl) {
        String normalized = normalizeFrontendBaseUrl(frontendBaseUrl);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeFrontendBaseUrl(String candidate) {
        String fallback = trimTrailingSlash(properties.getFrontendBaseUrl());
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        String normalized = trimTrailingSlash(candidate);
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getPath() != null && !uri.getPath().isBlank()) {
                return fallback;
            }
            return allowedFrontendBaseUrls().contains(normalized) ? normalized : fallback;
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Set<String> allowedFrontendBaseUrls() {
        Set<String> allowed = new LinkedHashSet<>();
        allowed.add(trimTrailingSlash(properties.getFrontendBaseUrl()));
        allowed.add(LOCALHOST_FRONTEND_BASE_URL);
        allowed.add(LOOPBACK_FRONTEND_BASE_URL);
        allowed.add("https://aura-ia.es");
        allowed.add("https://www.aura-ia.es");
        return allowed;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return LOCALHOST_FRONTEND_BASE_URL;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String displayName(GoogleOAuthUser googleUser, String email) {
        if (googleUser.name() != null && !googleUser.name().isBlank()) {
            return googleUser.name().trim();
        }
        int at = email.indexOf('@');
        return at > 1 ? email.substring(0, at) : "AURA User";
    }

    private boolean isConfiguredAdmin(String email) {
        return properties.getAdminEmails().stream()
            .map(this::normalizeEmail)
            .anyMatch(email::equals);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEnabled() {
        if (!properties.getGoogleOAuth().isEnabled()) {
            throw new BusinessException("error.google_oauth_unavailable");
        }
    }
}
