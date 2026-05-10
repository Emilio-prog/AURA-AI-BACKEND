package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 160) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        String captchaToken
    ) {
    }

    public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ResendVerificationRequest(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record ForgotPasswordRequest(
        @NotBlank @Email @Size(max = 320) String email,
        String captchaToken
    ) {
    }

    public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 128) String password
    ) {
    }
}
