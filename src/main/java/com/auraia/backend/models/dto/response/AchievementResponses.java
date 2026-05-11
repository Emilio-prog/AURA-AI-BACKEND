package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.AchievementCode;
import java.time.Instant;
import java.util.List;

public final class AchievementResponses {

    private AchievementResponses() {
    }

    public record AchievementListResponse(
        int total,
        int unlocked,
        List<AchievementResponse> achievements
    ) {
    }

    public record AchievementResponse(
        AchievementCode code,
        String title,
        String description,
        String category,
        String accent,
        int progress,
        int target,
        boolean unlocked,
        Instant unlockedAt,
        String progressLabel
    ) {
    }
}
