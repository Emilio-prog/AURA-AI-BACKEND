package com.auraia.backend.models.dto.response;

import java.time.Instant;

public final class AuthResponses {

    private AuthResponses() {
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresInMs;
        private UserResponses.UserResponse user;

        public AuthResponse() {
        }

        public AuthResponse(String accessToken, String refreshToken, String tokenType,
                            long expiresInMs, UserResponses.UserResponse user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.expiresInMs = expiresInMs;
            this.user = user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public long getExpiresInMs() {
            return expiresInMs;
        }

        public void setExpiresInMs(long expiresInMs) {
            this.expiresInMs = expiresInMs;
        }

        public UserResponses.UserResponse getUser() {
            return user;
        }

        public void setUser(UserResponses.UserResponse user) {
            this.user = user;
        }
    }

    public static class PendingVerificationResponse {
        private String email;
        private String message;
        private boolean requiresVerification;

        public PendingVerificationResponse() {
        }

        public PendingVerificationResponse(String email, String message, boolean requiresVerification) {
            this.email = email;
            this.message = message;
            this.requiresVerification = requiresVerification;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isRequiresVerification() {
            return requiresVerification;
        }

        public void setRequiresVerification(boolean requiresVerification) {
            this.requiresVerification = requiresVerification;
        }
    }

    public static class MessageResponse {
        private String message;

        public MessageResponse() {
        }

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class OAuthStartResponse {
        private String authorizationUrl;

        public OAuthStartResponse() {
        }

        public OAuthStartResponse(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }
    }

    public static class OAuthStatusResponse {
        private boolean linked;
        private String email;
        private Instant linkedAt;

        public OAuthStatusResponse() {
        }

        public OAuthStatusResponse(boolean linked, String email, Instant linkedAt) {
            this.linked = linked;
            this.email = email;
            this.linkedAt = linkedAt;
        }

        public boolean isLinked() {
            return linked;
        }

        public void setLinked(boolean linked) {
            this.linked = linked;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Instant getLinkedAt() {
            return linkedAt;
        }

        public void setLinkedAt(Instant linkedAt) {
            this.linkedAt = linkedAt;
        }
    }
}
