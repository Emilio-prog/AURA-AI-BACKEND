package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.NotificationStatus;
import com.auraia.backend.models.enums.Theme;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DomainResponses {

    private DomainResponses() {
    }

    public record DiaryEntryResponse(
        UUID id,
        String title,
        String content,
        Integer moodScore,
        String moodLabel,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record MoodLogResponse(
        UUID id,
        int beforeLevel,
        int afterLevel,
        String note,
        Instant loggedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record MoodStatsResponse(
        Instant from,
        Instant to,
        long count,
        double averageBefore,
        double averageAfter,
        double improvementPercentage,
        String trend
    ) {
    }

    public record ChatSessionResponse(
        UUID id,
        String title,
        List<Map<String, Object>> messages,
        Instant startedAt,
        Instant updatedAt
    ) {
    }

    public record ContactResponse(
        UUID id,
        String name,
        String phone,
        String relationship,
        int priority,
        boolean available,
        boolean sosEnabled,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record PanicAlertResponse(
        UUID id,
        Instant triggeredAt,
        Instant resolvedAt,
        String notes,
        Map<String, Object> contextJson,
        List<PanicNotificationResponse> notifications,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record PanicNotificationResponse(
        UUID id,
        UUID contactId,
        String contactName,
        String channel,
        NotificationStatus status,
        String details,
        Instant createdAt
    ) {
    }

    public record UserSettingsResponse(
        UUID id,
        Theme theme,
        String language,
        String timezone,
        Map<String, Object> notificationPreferences,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
