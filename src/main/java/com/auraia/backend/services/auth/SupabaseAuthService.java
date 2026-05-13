package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.entities.OAuthIdentity;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.OAuthIdentityRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupabaseAuthService {

    private static final String PROVIDER_SUPABASE = "SUPABASE";

    private final SupabaseAuthClient supabaseAuthClient;
    private final OAuthIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final AuthTokenService authTokenService;
    private final AppProperties properties;

    @Transactional
    public AuthResponses.AuthResponse exchange(AuthRequests.SupabaseExchangeRequest request) {
        SupabaseAuthUser supabaseUser = supabaseAuthClient.fetchUser(request.accessToken());
        if (!supabaseUser.emailVerified()) {
            throw new BusinessException("error.google_oauth_email_unverified");
        }

        User user = resolveUser(supabaseUser);
        linkIdentity(user, supabaseUser);
        return authTokenService.issueTokens(user);
    }

    private User resolveUser(SupabaseAuthUser supabaseUser) {
        return identityRepository.findByProviderAndProviderSubjectAndActiveTrue(PROVIDER_SUPABASE, supabaseUser.subject())
            .map(OAuthIdentity::getUser)
            .filter(user -> !user.isDeleted())
            .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizeEmail(supabaseUser.email()))
                .map(user -> {
                    if (user.isDeleted()) {
                        throw new BusinessException("error.google_oauth_email_conflict");
                    }
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                })
                .orElseGet(() -> createUser(supabaseUser)));
    }

    private void linkIdentity(User user, SupabaseAuthUser supabaseUser) {
        identityRepository.findByProviderAndProviderSubjectAndActiveTrue(PROVIDER_SUPABASE, supabaseUser.subject())
            .filter(existing -> !existing.getUser().getId().equals(user.getId()))
            .ifPresent(existing -> {
                throw new BusinessException("error.google_oauth_email_conflict");
            });

        OAuthIdentity identity = identityRepository.findByUserAndProviderAndActiveTrue(user, PROVIDER_SUPABASE)
            .orElse(null);
        if (identity != null && !identity.getProviderSubject().equals(supabaseUser.subject())) {
            throw new BusinessException("error.google_oauth_email_conflict");
        }

        if (identity == null) {
            identity = OAuthIdentity.builder()
                .user(user)
                .provider(PROVIDER_SUPABASE)
                .providerSubject(supabaseUser.subject())
                .active(true)
                .linkedAt(Instant.now())
                .build();
        }
        identity.setEmail(normalizeEmail(supabaseUser.email()));
        identity.setEmailVerified(supabaseUser.emailVerified());
        identityRepository.save(identity);
    }

    private User createUser(SupabaseAuthUser supabaseUser) {
        String email = normalizeEmail(supabaseUser.email());
        User user = User.builder()
            .email(email)
            .passwordHash(null)
            .name(displayName(supabaseUser, email))
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

    private String displayName(SupabaseAuthUser supabaseUser, String email) {
        if (supabaseUser.name() != null && !supabaseUser.name().isBlank()) {
            return supabaseUser.name().trim();
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
}
