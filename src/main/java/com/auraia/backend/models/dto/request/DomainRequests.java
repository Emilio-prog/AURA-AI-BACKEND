package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Theme;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DomainRequests {

    private DomainRequests() {
    }

    public record DiaryEntryRequest(
        @Size(max = 180) String title,
        @NotBlank @Size(max = 20000) String content,
        @Min(1) @Max(10) Integer moodScore,
        @Size(max = 80) String moodLabel,
        List<@Size(min = 0, max = 80) String> tags
    ) {
    }

    public record MoodLogRequest(
        @Min(1) @Max(10) int beforeLevel,
        @Min(1) @Max(10) int afterLevel,
        @Size(max = 2000) String note,
        Instant loggedAt
    ) {
    }

    public record ChatMessageRequest(@NotBlank @Size(max = 8000) String message) {
    }

    public record ContactRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 40) String phone,
        @Size(max = 80) String relationship,
        @Min(1) @Max(99) Integer priority,
        Boolean available,
        Boolean sosEnabled
    ) {
    }

    public record PanicTriggerRequest(
        @Size(max = 3000) String notes,
        Map<String, Object> contextJson
    ) {
    }

    public record PanicResolveRequest(@Size(max = 3000) String notes) {
    }

    public record UserSettingsRequest(
        Theme theme,
        @Size(min = 2, max = 16) String language,
        @Size(min = 2, max = 64) String timezone,
        @NotNull Map<String, Object> notificationPreferences
    ) {
    }
}
