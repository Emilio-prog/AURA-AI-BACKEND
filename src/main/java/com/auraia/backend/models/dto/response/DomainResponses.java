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

    public static class DiaryEntryResponse {
        private UUID id;
        private String title;
        private String content;
        private Integer moodScore;
        private String moodLabel;
        private List<String> tags;
        private Instant createdAt;
        private Instant updatedAt;

        public DiaryEntryResponse() {
        }

        public DiaryEntryResponse(UUID id, String title, String content, Integer moodScore,
                                  String moodLabel, List<String> tags, Instant createdAt,
                                  Instant updatedAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.moodScore = moodScore;
            this.moodLabel = moodLabel;
            this.tags = tags;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class MoodLogResponse {
        private UUID id;
        private int beforeLevel;
        private int afterLevel;
        private String note;
        private Instant loggedAt;
        private Instant createdAt;
        private Instant updatedAt;

        public MoodLogResponse() {
        }

        public MoodLogResponse(UUID id, int beforeLevel, int afterLevel, String note,
                               Instant loggedAt, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.beforeLevel = beforeLevel;
            this.afterLevel = afterLevel;
            this.note = note;
            this.loggedAt = loggedAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class MoodStatsResponse {
        private Instant from;
        private Instant to;
        private long count;
        private double averageBefore;
        private double averageAfter;
        private double improvementPercentage;
        private String trend;

        public MoodStatsResponse() {
        }

        public MoodStatsResponse(Instant from, Instant to, long count, double averageBefore,
                                 double averageAfter, double improvementPercentage, String trend) {
            this.from = from;
            this.to = to;
            this.count = count;
            this.averageBefore = averageBefore;
            this.averageAfter = averageAfter;
            this.improvementPercentage = improvementPercentage;
            this.trend = trend;
        }

        public Instant getFrom() {
            return from;
        }

        public void setFrom(Instant from) {
            this.from = from;
        }

        public Instant getTo() {
            return to;
        }

        public void setTo(Instant to) {
            this.to = to;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getAverageBefore() {
            return averageBefore;
        }

        public void setAverageBefore(double averageBefore) {
            this.averageBefore = averageBefore;
        }

        public double getAverageAfter() {
            return averageAfter;
        }

        public void setAverageAfter(double averageAfter) {
            this.averageAfter = averageAfter;
        }

        public double getImprovementPercentage() {
            return improvementPercentage;
        }

        public void setImprovementPercentage(double improvementPercentage) {
            this.improvementPercentage = improvementPercentage;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }
    }

    public static class ChatSessionResponse {
        private UUID id;
        private String title;
        private List<Map<String, Object>> messages;
        private Instant startedAt;
        private Instant updatedAt;

        public ChatSessionResponse() {
        }

        public ChatSessionResponse(UUID id, String title, List<Map<String, Object>> messages,
                                   Instant startedAt, Instant updatedAt) {
            this.id = id;
            this.title = title;
            this.messages = messages;
            this.startedAt = startedAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Map<String, Object>> getMessages() {
            return messages;
        }

        public void setMessages(List<Map<String, Object>> messages) {
            this.messages = messages;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Instant startedAt) {
            this.startedAt = startedAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ContactResponse {
        private UUID id;
        private String name;
        private String phone;
        private String relationship;
        private int priority;
        private boolean available;
        private boolean sosEnabled;
        private Instant createdAt;
        private Instant updatedAt;

        public ContactResponse() {
        }

        public ContactResponse(UUID id, String name, String phone, String relationship,
                               int priority, boolean available, boolean sosEnabled,
                               Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            this.priority = priority;
            this.available = available;
            this.sosEnabled = sosEnabled;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public boolean isSosEnabled() {
            return sosEnabled;
        }

        public void setSosEnabled(boolean sosEnabled) {
            this.sosEnabled = sosEnabled;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class PanicAlertResponse {
        private UUID id;
        private Instant triggeredAt;
        private Instant resolvedAt;
        private String notes;
        private Map<String, Object> contextJson;
        private List<PanicNotificationResponse> notifications;
        private Instant createdAt;
        private Instant updatedAt;

        public PanicAlertResponse() {
        }

        public PanicAlertResponse(UUID id, Instant triggeredAt, Instant resolvedAt,
                                  String notes, Map<String, Object> contextJson,
                                  List<PanicNotificationResponse> notifications,
                                  Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.triggeredAt = triggeredAt;
            this.resolvedAt = resolvedAt;
            this.notes = notes;
            this.contextJson = contextJson;
            this.notifications = notifications;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public Instant getTriggeredAt() {
            return triggeredAt;
        }

        public void setTriggeredAt(Instant triggeredAt) {
            this.triggeredAt = triggeredAt;
        }

        public Instant getResolvedAt() {
            return resolvedAt;
        }

        public void setResolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Map<String, Object> getContextJson() {
            return contextJson;
        }

        public void setContextJson(Map<String, Object> contextJson) {
            this.contextJson = contextJson;
        }

        public List<PanicNotificationResponse> getNotifications() {
            return notifications;
        }

        public void setNotifications(List<PanicNotificationResponse> notifications) {
            this.notifications = notifications;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class PanicNotificationResponse {
        private UUID id;
        private UUID contactId;
        private String contactName;
        private String channel;
        private NotificationStatus status;
        private String details;
        private Instant createdAt;

        public PanicNotificationResponse() {
        }

        public PanicNotificationResponse(UUID id, UUID contactId, String contactName,
                                         String channel, NotificationStatus status,
                                         String details, Instant createdAt) {
            this.id = id;
            this.contactId = contactId;
            this.contactName = contactName;
            this.channel = channel;
            this.status = status;
            this.details = details;
            this.createdAt = createdAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getContactId() {
            return contactId;
        }

        public void setContactId(UUID contactId) {
            this.contactId = contactId;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public NotificationStatus getStatus() {
            return status;
        }

        public void setStatus(NotificationStatus status) {
            this.status = status;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class UserSettingsResponse {
        private UUID id;
        private Theme theme;
        private String language;
        private String timezone;
        private Map<String, Object> notificationPreferences;
        private Instant createdAt;
        private Instant updatedAt;

        public UserSettingsResponse() {
        }

        public UserSettingsResponse(UUID id, Theme theme, String language, String timezone,
                                    Map<String, Object> notificationPreferences,
                                    Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.theme = theme;
            this.language = language;
            this.timezone = timezone;
            this.notificationPreferences = notificationPreferences;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
