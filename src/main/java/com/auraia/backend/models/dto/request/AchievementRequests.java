package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.AchievementEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public final class AchievementRequests {

    private AchievementRequests() {
    }

    public record EventRequest(
        @NotNull AchievementEventType type,
        @NotBlank @Size(max = 160) String idempotencyKey,
        Instant occurredAt,
        Map<String, Object> metadata
    ) {
    }
}
