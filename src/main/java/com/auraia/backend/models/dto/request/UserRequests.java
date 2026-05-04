package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
}
