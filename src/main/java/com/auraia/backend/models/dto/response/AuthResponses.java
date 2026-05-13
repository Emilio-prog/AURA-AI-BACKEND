package com.auraia.backend.models.dto.response;

import java.time.Instant;

public final class AuthResponses {

    private AuthResponses() {
    }

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserResponses.UserResponse user
    ) {
    }

    public record PendingVerificationResponse(String email, String message, boolean requiresVerification) {
    }

    public record MessageResponse(String message) {
    }

    public record OAuthStartResponse(String authorizationUrl) {
    }

    public record OAuthStatusResponse(boolean linked, String email, Instant linkedAt) {
    }
}
