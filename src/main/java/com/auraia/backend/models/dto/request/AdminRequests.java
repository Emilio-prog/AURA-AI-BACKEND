package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;

public final class AdminRequests {

    private AdminRequests() {
    }

    public record AdminUpdateUserRequest(
        Role role,
        Plan plan,
        Boolean emailVerified
    ) {
    }
}
