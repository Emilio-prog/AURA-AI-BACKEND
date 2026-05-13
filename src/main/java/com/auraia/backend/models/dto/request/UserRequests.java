package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

public final class UserRequests {

    private UserRequests() {
    }

    public record UpdateUserRequest(
        @Size(min = 2, max = 160) String name,
        @Email @Size(max = 320) String email
    ) {
    }

    public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 12, max = 128) String newPassword
    ) {
    }

    public record DeleteAccountRequest(
        @NotBlank @Size(max = 80) String confirmationText,
        @Size(max = 128) String currentPassword
    ) {
    }

    public record CompleteOnboardingRequest(
        @NotBlank @Size(min = 2, max = 160) String preferredName,
        @NotBlank @Size(min = 2, max = 16) String language,
        @NotBlank @Size(min = 2, max = 64) String timezone,
        @NotNull Boolean privacyAccepted,
        @NotNull Boolean termsAccepted,
        @NotNull Boolean supportOnlyAccepted,
        @NotNull Boolean ageConfirmed,
        @Size(max = 12) List<@Size(max = 40) String> goals,
        @Size(max = 12) List<@Size(max = 40) String> anxietyTriggers,
        @Valid CurrentMoodRequest currentMood,
        @Size(max = 12) List<@Size(max = 40) String> toolPreferences,
        Map<String, Object> notifications,
        @Valid TrustedContactRequest trustedContact
    ) {
    }

    public record CurrentMoodRequest(
        @NotBlank @Size(max = 80) String label,
        @Min(1) @Max(10) int intensity
    ) {
    }

    public record TrustedContactRequest(
        @Size(max = 160) String name,
        @Size(max = 40) String phone,
        @Size(max = 80) String relationship
    ) {
    }
}
