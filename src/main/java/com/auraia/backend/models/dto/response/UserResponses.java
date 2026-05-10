package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UserResponses {

    private UserResponses() {
    }

    public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Plan plan,
        boolean emailVerified,
        Instant createdAt,
        Instant onboardedAt
    ) {
    }

    public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Plan plan,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        Instant onboardedAt,
        Instant deletedAt
    ) {
    }

    public record ExportDataResponse(
        Instant exportedAt,
        UserResponse profile,
        DomainResponses.UserSettingsResponse settings,
        List<DomainResponses.DiaryEntryResponse> diary,
        List<DomainResponses.MoodLogResponse> moods,
        List<DomainResponses.ChatSessionResponse> chatSessions,
        List<DomainResponses.ContactResponse> contacts,
        List<DomainResponses.PanicAlertResponse> panicAlerts,
        Map<String, Object> metadata
    ) {
    }
}
