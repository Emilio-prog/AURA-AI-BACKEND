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
import java.util.UUID;

public final class DomainRequests {

    private DomainRequests() {
    }

    public static class DiaryEntryRequest {
        @Size(max = 180)
        private String title;

        @NotBlank
        @Size(max = 20000)
        private String content;

        @Min(1)
        @Max(10)
        private Integer moodScore;

        @Size(max = 80)
        private String moodLabel;

        private List<@Size(min = 0, max = 80) String> tags;

        public DiaryEntryRequest() {
        }

        public DiaryEntryRequest(String title, String content, Integer moodScore, String moodLabel, List<String> tags) {
            this.title = title;
            this.content = content;
            this.moodScore = moodScore;
            this.moodLabel = moodLabel;
            this.tags = tags;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getMoodScore() {
            return moodScore;
        }

        public void setMoodScore(Integer moodScore) {
            this.moodScore = moodScore;
        }

        public String getMoodLabel() {
            return moodLabel;
        }

        public void setMoodLabel(String moodLabel) {
            this.moodLabel = moodLabel;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class MoodLogRequest {
        @Min(1)
        @Max(10)
        private int beforeLevel;

        @Min(1)
        @Max(10)
        private int afterLevel;

        @Size(max = 2000)
        private String note;

        private Instant loggedAt;

        public MoodLogRequest() {
        }

        public MoodLogRequest(int beforeLevel, int afterLevel, String note, Instant loggedAt) {
            this.beforeLevel = beforeLevel;
            this.afterLevel = afterLevel;
            this.note = note;
            this.loggedAt = loggedAt;
        }

        public int getBeforeLevel() {
            return beforeLevel;
        }

        public void setBeforeLevel(int beforeLevel) {
            this.beforeLevel = beforeLevel;
        }

        public int getAfterLevel() {
            return afterLevel;
        }

        public void setAfterLevel(int afterLevel) {
            this.afterLevel = afterLevel;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public Instant getLoggedAt() {
            return loggedAt;
        }

        public void setLoggedAt(Instant loggedAt) {
            this.loggedAt = loggedAt;
        }
    }

    public static class ChatMessageRequest {
        @NotBlank
        @Size(max = 8000)
        private String message;

        public ChatMessageRequest() {
        }

        public ChatMessageRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ContactRequest {
        @NotBlank
        @Size(max = 160)
        private String name;

        @NotBlank
        @Size(max = 40)
        private String phone;

        @Size(max = 80)
        private String relationship;

        @Min(1)
        @Max(99)
        private Integer priority;

        private Boolean available;
        private Boolean sosEnabled;

        public ContactRequest() {
        }

        public ContactRequest(String name, String phone, String relationship, Integer priority, Boolean available, Boolean sosEnabled) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            this.priority = priority;
            this.available = available;
            this.sosEnabled = sosEnabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getRelationship() {
            return relationship;
        }

        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }

        public Boolean getSosEnabled() {
            return sosEnabled;
        }

        public void setSosEnabled(Boolean sosEnabled) {
            this.sosEnabled = sosEnabled;
        }
    }

    public static class PanicTriggerRequest {
        @Size(max = 3000)
        private String notes;

        private UUID contactId;
        private Map<String, Object> contextJson;

        public PanicTriggerRequest() {
        }

        public PanicTriggerRequest(String notes, UUID contactId, Map<String, Object> contextJson) {
            this.notes = notes;
            this.contactId = contactId;
            this.contextJson = contextJson;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public UUID getContactId() {
            return contactId;
        }

        public void setContactId(UUID contactId) {
            this.contactId = contactId;
        }

        public Map<String, Object> getContextJson() {
            return contextJson;
        }

        public void setContextJson(Map<String, Object> contextJson) {
            this.contextJson = contextJson;
        }
    }

    public static class PanicResolveRequest {
        @Size(max = 3000)
        private String notes;

        public PanicResolveRequest() {
        }

        public PanicResolveRequest(String notes) {
            this.notes = notes;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class UserSettingsRequest {
        private Theme theme;

        @Size(min = 2, max = 16)
        private String language;

        @Size(min = 2, max = 64)
        private String timezone;

        @NotNull
        private Map<String, Object> notificationPreferences;

        public UserSettingsRequest() {
        }

        public UserSettingsRequest(Theme theme, String language, String timezone, Map<String, Object> notificationPreferences) {
            this.theme = theme;
            this.language = language;
            this.timezone = timezone;
            this.notificationPreferences = notificationPreferences;
        }

        public Theme getTheme() {
            return theme;
        }

        public void setTheme(Theme theme) {
            this.theme = theme;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public Map<String, Object> getNotificationPreferences() {
            return notificationPreferences;
        }

        public void setNotificationPreferences(Map<String, Object> notificationPreferences) {
            this.notificationPreferences = notificationPreferences;
        }
    }
}
