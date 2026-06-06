package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

public final class UserRequests {

    private UserRequests() {
    }

    public static class UpdateUserRequest {
        @Size(min = 2, max = 160)
        private String name;

        @Email
        @Size(max = 320)
        private String email;

        public UpdateUserRequest() {
        }

        public UpdateUserRequest(String name, String email) {
            this.name = name;
            this.email = email;
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
    }

    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 12, max = 128)
        private String newPassword;

        public ChangePasswordRequest() {
        }

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class DeleteAccountRequest {
        @NotBlank
        @Size(max = 80)
        private String confirmationText;

        public DeleteAccountRequest() {
        }

        public DeleteAccountRequest(String confirmationText) {
            this.confirmationText = confirmationText;
        }

        public String getConfirmationText() {
            return confirmationText;
        }

        public void setConfirmationText(String confirmationText) {
            this.confirmationText = confirmationText;
        }
    }

    public static class CompleteOnboardingRequest {
        @NotBlank
        @Size(min = 2, max = 160)
        private String preferredName;

        @NotBlank
        @Size(min = 2, max = 16)
        private String language;

        @NotBlank
        @Size(min = 2, max = 64)
        private String timezone;

        @NotNull
        private Boolean privacyAccepted;

        @NotNull
        private Boolean termsAccepted;

        @NotNull
        private Boolean supportOnlyAccepted;

        @NotNull
        private Boolean ageConfirmed;

        @Size(max = 12)
        private List<@Size(max = 40) String> goals;

        @Size(max = 12)
        private List<@Size(max = 40) String> anxietyTriggers;

        @Valid
        private CurrentMoodRequest currentMood;

        @Size(max = 12)
        private List<@Size(max = 40) String> toolPreferences;

        private Map<String, Object> notifications;

        @Valid
        private TrustedContactRequest trustedContact;

        public CompleteOnboardingRequest() {
        }

        public CompleteOnboardingRequest(String preferredName, String language, String timezone,
                                         Boolean privacyAccepted, Boolean termsAccepted,
                                         Boolean supportOnlyAccepted, Boolean ageConfirmed,
                                         List<String> goals, List<String> anxietyTriggers,
                                         CurrentMoodRequest currentMood, List<String> toolPreferences,
                                         Map<String, Object> notifications, TrustedContactRequest trustedContact) {
            this.preferredName = preferredName;
            this.language = language;
            this.timezone = timezone;
            this.privacyAccepted = privacyAccepted;
            this.termsAccepted = termsAccepted;
            this.supportOnlyAccepted = supportOnlyAccepted;
            this.ageConfirmed = ageConfirmed;
            this.goals = goals;
            this.anxietyTriggers = anxietyTriggers;
            this.currentMood = currentMood;
            this.toolPreferences = toolPreferences;
            this.notifications = notifications;
            this.trustedContact = trustedContact;
        }

        public String getPreferredName() {
            return preferredName;
        }

        public void setPreferredName(String preferredName) {
            this.preferredName = preferredName;
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

        public Boolean getPrivacyAccepted() {
            return privacyAccepted;
        }

        public void setPrivacyAccepted(Boolean privacyAccepted) {
            this.privacyAccepted = privacyAccepted;
        }

        public Boolean getTermsAccepted() {
            return termsAccepted;
        }

        public void setTermsAccepted(Boolean termsAccepted) {
            this.termsAccepted = termsAccepted;
        }

        public Boolean getSupportOnlyAccepted() {
            return supportOnlyAccepted;
        }

        public void setSupportOnlyAccepted(Boolean supportOnlyAccepted) {
            this.supportOnlyAccepted = supportOnlyAccepted;
        }

        public Boolean getAgeConfirmed() {
            return ageConfirmed;
        }

        public void setAgeConfirmed(Boolean ageConfirmed) {
            this.ageConfirmed = ageConfirmed;
        }

        public List<String> getGoals() {
            return goals;
        }

        public void setGoals(List<String> goals) {
            this.goals = goals;
        }

        public List<String> getAnxietyTriggers() {
            return anxietyTriggers;
        }

        public void setAnxietyTriggers(List<String> anxietyTriggers) {
            this.anxietyTriggers = anxietyTriggers;
        }

        public CurrentMoodRequest getCurrentMood() {
            return currentMood;
        }

        public void setCurrentMood(CurrentMoodRequest currentMood) {
            this.currentMood = currentMood;
        }

        public List<String> getToolPreferences() {
            return toolPreferences;
        }

        public void setToolPreferences(List<String> toolPreferences) {
            this.toolPreferences = toolPreferences;
        }

        public Map<String, Object> getNotifications() {
            return notifications;
        }

        public void setNotifications(Map<String, Object> notifications) {
            this.notifications = notifications;
        }

        public TrustedContactRequest getTrustedContact() {
            return trustedContact;
        }

        public void setTrustedContact(TrustedContactRequest trustedContact) {
            this.trustedContact = trustedContact;
        }
    }

    public static class CurrentMoodRequest {
        @NotBlank
        @Size(max = 80)
        private String label;

        @Min(1)
        @Max(10)
        private int intensity;

        public CurrentMoodRequest() {
        }

        public CurrentMoodRequest(String label, int intensity) {
            this.label = label;
            this.intensity = intensity;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getIntensity() {
            return intensity;
        }

        public void setIntensity(int intensity) {
            this.intensity = intensity;
        }
    }

    public static class TrustedContactRequest {
        @Size(max = 160)
        private String name;

        @Size(max = 40)
        private String phone;

        @Size(max = 80)
        private String relationship;

        public TrustedContactRequest() {
        }

        public TrustedContactRequest(String name, String phone, String relationship) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
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
    }
}
