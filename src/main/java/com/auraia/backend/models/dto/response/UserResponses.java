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

    public static class UserResponse {
        private UUID id;
        private String name;
        private String email;
        private Role role;
        private Plan plan;
        private boolean emailVerified;
        private Instant createdAt;
        private Instant onboardedAt;

        public UserResponse() {
        }

        public UserResponse(UUID id, String name, String email, Role role, Plan plan,
                            boolean emailVerified, Instant createdAt, Instant onboardedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.plan = plan;
            this.emailVerified = emailVerified;
            this.createdAt = createdAt;
            this.onboardedAt = onboardedAt;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Plan getPlan() {
            return plan;
        }

        public void setPlan(Plan plan) {
            this.plan = plan;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getOnboardedAt() {
            return onboardedAt;
        }

        public void setOnboardedAt(Instant onboardedAt) {
            this.onboardedAt = onboardedAt;
        }
    }

    public static class AdminUserResponse {
        private UUID id;
        private String name;
        private String email;
        private Role role;
        private Plan plan;
        private boolean emailVerified;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant onboardedAt;
        private Instant deletedAt;

        public AdminUserResponse() {
        }

        public AdminUserResponse(UUID id, String name, String email, Role role, Plan plan,
                                 boolean emailVerified, Instant createdAt, Instant updatedAt,
                                 Instant onboardedAt, Instant deletedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.plan = plan;
            this.emailVerified = emailVerified;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.onboardedAt = onboardedAt;
            this.deletedAt = deletedAt;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Plan getPlan() {
            return plan;
        }

        public void setPlan(Plan plan) {
            this.plan = plan;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
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

        public Instant getOnboardedAt() {
            return onboardedAt;
        }

        public void setOnboardedAt(Instant onboardedAt) {
            this.onboardedAt = onboardedAt;
        }

        public Instant getDeletedAt() {
            return deletedAt;
        }

        public void setDeletedAt(Instant deletedAt) {
            this.deletedAt = deletedAt;
        }
    }

    public static class ExportDataResponse {
        private Instant exportedAt;
        private UserResponse profile;
        private DomainResponses.UserSettingsResponse settings;
        private List<DomainResponses.DiaryEntryResponse> diary;
        private List<DomainResponses.MoodLogResponse> moods;
        private List<DomainResponses.ChatSessionResponse> chatSessions;
        private List<DomainResponses.ContactResponse> contacts;
        private List<DomainResponses.PanicAlertResponse> panicAlerts;
        private Map<String, Object> metadata;

        public ExportDataResponse() {
        }

        public ExportDataResponse(Instant exportedAt, UserResponse profile,
                                  DomainResponses.UserSettingsResponse settings,
                                  List<DomainResponses.DiaryEntryResponse> diary,
                                  List<DomainResponses.MoodLogResponse> moods,
                                  List<DomainResponses.ChatSessionResponse> chatSessions,
                                  List<DomainResponses.ContactResponse> contacts,
                                  List<DomainResponses.PanicAlertResponse> panicAlerts,
                                  Map<String, Object> metadata) {
            this.exportedAt = exportedAt;
            this.profile = profile;
            this.settings = settings;
            this.diary = diary;
            this.moods = moods;
            this.chatSessions = chatSessions;
            this.contacts = contacts;
            this.panicAlerts = panicAlerts;
            this.metadata = metadata;
        }

        public Instant getExportedAt() {
            return exportedAt;
        }

        public void setExportedAt(Instant exportedAt) {
            this.exportedAt = exportedAt;
        }

        public UserResponse getProfile() {
            return profile;
        }

        public void setProfile(UserResponse profile) {
            this.profile = profile;
        }

        public DomainResponses.UserSettingsResponse getSettings() {
            return settings;
        }

        public void setSettings(DomainResponses.UserSettingsResponse settings) {
            this.settings = settings;
        }

        public List<DomainResponses.DiaryEntryResponse> getDiary() {
            return diary;
        }

        public void setDiary(List<DomainResponses.DiaryEntryResponse> diary) {
            this.diary = diary;
        }

        public List<DomainResponses.MoodLogResponse> getMoods() {
            return moods;
        }

        public void setMoods(List<DomainResponses.MoodLogResponse> moods) {
            this.moods = moods;
        }

        public List<DomainResponses.ChatSessionResponse> getChatSessions() {
            return chatSessions;
        }

        public void setChatSessions(List<DomainResponses.ChatSessionResponse> chatSessions) {
            this.chatSessions = chatSessions;
        }

        public List<DomainResponses.ContactResponse> getContacts() {
            return contacts;
        }

        public void setContacts(List<DomainResponses.ContactResponse> contacts) {
            this.contacts = contacts;
        }

        public List<DomainResponses.PanicAlertResponse> getPanicAlerts() {
            return panicAlerts;
        }

        public void setPanicAlerts(List<DomainResponses.PanicAlertResponse> panicAlerts) {
            this.panicAlerts = panicAlerts;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
