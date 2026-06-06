package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class DefaultGoogleOAuthClient implements GoogleOAuthClient {

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPES = "openid email profile";

    private final AppProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    @PostConstruct
    void initialize() {
        AppProperties.GoogleOAuth config = properties.getGoogleOAuth();
        if (!config.isEnabled()) {
            return;
        }
        if (isBlank(config.getClientId()) || isBlank(config.getClientSecret()) || isBlank(config.getRedirectUri())) {
            throw new IllegalStateException("GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET and GOOGLE_OAUTH_REDIRECT_URI are required when GOOGLE_OAUTH_ENABLED=true");
        }
    }

    @Override
    public String authorizationUrl(String state) {
        ensureEnabled();
        AppProperties.GoogleOAuth config = properties.getGoogleOAuth();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPES)
            .queryParam("state", state)
            .queryParam("prompt", "select_account")
            .build()
            .toUriString();
    }

    @Override
    public GoogleOAuthUser fetchUser(String code) {
        ensureEnabled();
        AppProperties.GoogleOAuth config = properties.getGoogleOAuth();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", config.getRedirectUri());

        GoogleTokenResponse token = restClient.post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(GoogleTokenResponse.class);

        if (token == null || isBlank(token.getAccessToken())) {
            throw new BusinessException("error.google_oauth_failed");
        }

        GoogleUserInfoResponse userInfo = restClient.get()
            .uri(USERINFO_URL)
            .headers(headers -> headers.setBearerAuth(token.getAccessToken()))
            .retrieve()
            .body(GoogleUserInfoResponse.class);

        if (userInfo == null || isBlank(userInfo.getSubject()) || isBlank(userInfo.getEmail())) {
            throw new BusinessException("error.google_oauth_failed");
        }
        return new GoogleOAuthUser(
            userInfo.getSubject(),
            userInfo.getEmail(),
            Boolean.TRUE.equals(userInfo.getEmailVerified()),
            userInfo.getName()
        );
    }

    private void ensureEnabled() {
        if (!properties.getGoogleOAuth().isEnabled()) {
            throw new BusinessException("error.google_oauth_unavailable");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static class GoogleTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        private GoogleTokenResponse() {
        }

        private GoogleTokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        private String getAccessToken() {
            return accessToken;
        }

        private void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    private static class GoogleUserInfoResponse {
        @JsonProperty("sub")
        private String subject;

        private String email;

        @JsonProperty("email_verified")
        private Boolean emailVerified;

        private String name;

        private GoogleUserInfoResponse() {
        }

        private GoogleUserInfoResponse(String subject, String email, Boolean emailVerified, String name) {
            this.subject = subject;
            this.email = email;
            this.emailVerified = emailVerified;
            this.name = name;
        }

        private String getSubject() {
            return subject;
        }

        private void setSubject(String subject) {
            this.subject = subject;
        }

        private String getEmail() {
            return email;
        }

        private void setEmail(String email) {
            this.email = email;
        }

        private Boolean getEmailVerified() {
            return emailVerified;
        }

        private void setEmailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
        }

        private String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }
    }
}
