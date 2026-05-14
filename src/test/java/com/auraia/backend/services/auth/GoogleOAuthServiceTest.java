package com.auraia.backend.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.models.entities.OAuthExchangeCode;
import com.auraia.backend.models.entities.OAuthIdentity;
import com.auraia.backend.models.entities.OAuthState;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.repositories.OAuthExchangeCodeRepository;
import com.auraia.backend.repositories.OAuthIdentityRepository;
import com.auraia.backend.repositories.OAuthStateRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.UserPrincipal;
import com.auraia.backend.utils.TokenHashing;
import java.time.Instant;
import java.util.List;
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
class GoogleOAuthServiceTest {

    @Mock
    GoogleOAuthClient googleOAuthClient;
    @Mock
    OAuthStateRepository stateRepository;
    @Mock
    OAuthIdentityRepository identityRepository;
    @Mock
    OAuthExchangeCodeRepository exchangeCodeRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    AuthTokenService authTokenService;

    AppProperties properties;
    GoogleOAuthService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setFrontendBaseUrl("http://localhost:5173");
        properties.getGoogleOAuth().setEnabled(true);
        properties.getGoogleOAuth().setStateTtlMinutes(10);
        properties.getGoogleOAuth().setExchangeCodeTtlMinutes(5);
        service = new GoogleOAuthService(
            properties,
            googleOAuthClient,
            stateRepository,
            identityRepository,
            exchangeCodeRepository,
            userRepository,
            userSettingsRepository,
            authTokenService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startLoginStoresHashedStateAndReturnsAuthorizationUrl() {
        when(stateRepository.save(any(OAuthState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(googleOAuthClient.authorizationUrl(any())).thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=raw");

        AuthResponses.OAuthStartResponse response = service.startLogin();

        assertThat(response.authorizationUrl()).contains("accounts.google.com");
        ArgumentCaptor<String> rawState = ArgumentCaptor.forClass(String.class);
        verify(googleOAuthClient).authorizationUrl(rawState.capture());
        ArgumentCaptor<OAuthState> state = ArgumentCaptor.forClass(OAuthState.class);
        verify(stateRepository).save(state.capture());
        assertThat(state.getValue().getStateHash()).isEqualTo(TokenHashing.sha256(rawState.getValue()));
        assertThat(state.getValue().getFlow()).isEqualTo("LOGIN");
    }

    @Test
    void callbackCreatesGoogleUserAndExchangeCode() {
        OAuthState state = OAuthState.builder()
            .stateHash(TokenHashing.sha256("raw-state"))
            .flow("LOGIN")
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(stateRepository.findByStateHash(TokenHashing.sha256("raw-state"))).thenReturn(Optional.of(state));
        when(googleOAuthClient.fetchUser("google-code")).thenReturn(new GoogleOAuthUser(
            "google-sub",
            "New.User@Example.com",
            true,
            "New User"
        ));
        when(identityRepository.findByProviderAndProviderSubjectAndActiveTrue("GOOGLE", "google-sub"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(identityRepository.findByUserAndProviderAndActiveTrue(any(User.class), any())).thenReturn(Optional.empty());

        String redirect = service.handleCallback("raw-state", "google-code", null);

        assertThat(redirect).startsWith("http://localhost:5173/#/auth/google/callback?code=");
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("new.user@example.com");
        assertThat(user.getValue().getPasswordHash()).isNull();
        assertThat(user.getValue().isEmailVerified()).isTrue();
        verify(userSettingsRepository).save(any(UserSettings.class));
        verify(identityRepository).save(any(OAuthIdentity.class));
        verify(exchangeCodeRepository).save(any(OAuthExchangeCode.class));
    }

    @Test
    void callbackRejectsUnverifiedGoogleEmail() {
        OAuthState state = OAuthState.builder()
            .stateHash(TokenHashing.sha256("raw-state"))
            .flow("LOGIN")
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(stateRepository.findByStateHash(TokenHashing.sha256("raw-state"))).thenReturn(Optional.of(state));
        when(googleOAuthClient.fetchUser("google-code")).thenReturn(new GoogleOAuthUser(
            "google-sub",
            "new@example.com",
            false,
            "New User"
        ));

        String redirect = service.handleCallback("raw-state", "google-code", null);

        assertThat(redirect).contains("error=");
        verify(userRepository, never()).save(any(User.class));
        verify(exchangeCodeRepository, never()).save(any(OAuthExchangeCode.class));
    }

    @Test
    void exchangeConsumesCodeAndIssuesAuraTokens() {
        User user = user("emilio@example.com", "hash");
        OAuthExchangeCode exchangeCode = OAuthExchangeCode.builder()
            .user(user)
            .codeHash(TokenHashing.sha256("exchange-code"))
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(exchangeCodeRepository.findByCodeHash(TokenHashing.sha256("exchange-code")))
            .thenReturn(Optional.of(exchangeCode));
        AuthResponses.AuthResponse auth = new AuthResponses.AuthResponse(
            "access",
            "refresh",
            "Bearer",
            900000,
            new UserResponses.UserResponse(user.getId(), "Emilio", user.getEmail(), Role.USER, Plan.FREE, true, Instant.now(), null)
        );
        when(authTokenService.issueTokens(user)).thenReturn(auth);

        AuthResponses.AuthResponse response = service.exchange(new AuthRequests.OAuthExchangeRequest("exchange-code"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(exchangeCode.getConsumedAt()).isNotNull();
        verify(exchangeCodeRepository).save(exchangeCode);
    }

    @Test
    void unlinkRejectsWhenGoogleIsOnlyLoginMethod() {
        User user = user("emilio@example.com", null);
        authenticate(user);
        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(identityRepository.findByUserAndProviderAndActiveTrue(user, "GOOGLE"))
            .thenReturn(Optional.of(OAuthIdentity.builder()
                .user(user)
                .provider("GOOGLE")
                .providerSubject("sub")
                .email(user.getEmail())
                .active(true)
                .linkedAt(Instant.now())
                .build()));

        assertThatThrownBy(() -> service.unlink())
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.google_oauth_password_required");
    }

    private User user(String email, String passwordHash) {
        User user = User.builder()
            .email(email)
            .passwordHash(passwordHash)
            .name("Emilio")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
    }
}
