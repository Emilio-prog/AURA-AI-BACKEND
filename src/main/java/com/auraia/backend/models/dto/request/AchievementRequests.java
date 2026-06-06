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

    public static class EventRequest {
        @NotNull
        private AchievementEventType type;

        @NotBlank
        @Size(max = 160)
        private String idempotencyKey;

        private Instant occurredAt;

        private Map<String, Object> metadata;

        public EventRequest() {
        }

        public EventRequest(AchievementEventType type, String idempotencyKey, Instant occurredAt, Map<String, Object> metadata) {
            this.type = type;
            this.idempotencyKey = idempotencyKey;
            this.occurredAt = occurredAt;
            this.metadata = metadata;
        }

        public AchievementEventType getType() {
            return type;
        }

        public void setType(AchievementEventType type) {
            this.type = type;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public Instant getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
