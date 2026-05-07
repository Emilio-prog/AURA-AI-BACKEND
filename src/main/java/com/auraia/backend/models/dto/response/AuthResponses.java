package com.auraia.backend.models.dto.response;

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
}
