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

/**
 * Peticiones relacionadas con el usuario y su configuración inicial.
 * Aquí llegan datos del perfil, onboarding y cambios de contraseña.
 */
public final class UserRequests {

    private UserRequests() {
    }

    /**
     * Revisa textos que no pueden venir vacíos.
     */
    private static void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
    }

    /**
     * Comprueba un texto si viene informado. Si no viene, lo deja pasar.
     */
    private static void validarTextoOpcional(String valor, String campo, int minimo, int maximo) {
        if (valor == null) {
            return;
        }
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
        if (valor.length() < minimo || valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " tiene una longitud incorrecta");
        }
    }

    /**
     * Controla que un texto no sea demasiado largo.
     */
    private static void validarLongitud(String valor, String campo, int maximo) {
        if (valor != null && valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " es demasiado largo");
        }
    }

    /**
     * Mira de forma simple que el email tenga una arroba.
     */
    private static void validarEmail(String email) {
        if (email != null && !email.contains("@")) {
            throw new IllegalArgumentException("email no tiene un formato valido");
        }
    }

    /**
     * Datos que se pueden cambiar desde el perfil del usuario.
     */
    public static class UpdateUserRequest {
        @Size(min = 2, max = 160, message = "name tiene una longitud incorrecta")
        private String name;

        @Email(message = "email no tiene un formato valido")
        @Size(max = 320, message = "email es demasiado largo")
        private String email;

        public UpdateUserRequest() {
        }

        /**
         * Crea la petición y revisa nombre y email si vienen rellenos.
         */
        public UpdateUserRequest(String name, String email) {
            this.name = name;
            this.email = email;
            validarTextoOpcional(this.name, "name", 2, 160);
            validarLongitud(this.email, "email", 320);
            validarEmail(this.email);
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
        @NotBlank(message = "currentPassword no puede estar vacio")
        private String currentPassword;

        @NotBlank(message = "newPassword no puede estar vacio")
        @Size(min = 12, max = 128, message = "newPassword tiene una longitud incorrecta")
        private String newPassword;

        public ChangePasswordRequest() {
        }

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
            validarTextoObligatorio(this.currentPassword, "currentPassword");
            validarTextoObligatorio(this.newPassword, "newPassword");
            validarTextoOpcional(this.newPassword, "newPassword", 12, 128);
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
        @NotBlank(message = "confirmationText no puede estar vacio")
        @Size(max = 80, message = "confirmationText es demasiado largo")
        private String confirmationText;

        public DeleteAccountRequest() {
        }

        public DeleteAccountRequest(String confirmationText) {
            this.confirmationText = confirmationText;
            validarTextoObligatorio(this.confirmationText, "confirmationText");
            validarLongitud(this.confirmationText, "confirmationText", 80);
        }

        public String getConfirmationText() {
            return confirmationText;
        }

        public void setConfirmationText(String confirmationText) {
            this.confirmationText = confirmationText;
        }
    }

    public static class CompleteOnboardingRequest {
        @NotBlank(message = "El nombre no puede estar vacio")
        private String preferredName;

        @NotBlank(message = "language no puede estar vacio")
        @Size(min = 2, max = 16, message = "language tiene una longitud incorrecta")
        private String language;

        @NotBlank(message = "timezone no puede estar vacio")
        @Size(min = 2, max = 64, message = "timezone tiene una longitud incorrecta")
        private String timezone;

        @NotNull(message = "privacyAccepted no puede ser nulo")
        private Boolean privacyAccepted;

        @NotNull(message = "termsAccepted no puede ser nulo")
        private Boolean termsAccepted;

        @NotNull(message = "supportOnlyAccepted no puede ser nulo")
        private Boolean supportOnlyAccepted;

        @NotNull(message = "ageConfirmed no puede ser nulo")
        private Boolean ageConfirmed;

        @Size(max = 12, message = "goals tiene demasiados elementos")
        private List<@Size(max = 40, message = "goals tiene una longitud incorrecta") String> goals;

        @Size(max = 12, message = "anxietyTriggers tiene demasiados elementos")
        private List<@Size(max = 40, message = "anxietyTriggers tiene una longitud incorrecta") String> anxietyTriggers;

        @Valid
        private CurrentMoodRequest currentMood;

        @Size(max = 12, message = "toolPreferences tiene demasiados elementos")
        private List<@Size(max = 40, message = "toolPreferences tiene una longitud incorrecta") String> toolPreferences;

        private Map<String, Object> notifications;

        @Valid
        private TrustedContactRequest trustedContact;

        public CompleteOnboardingRequest() {
        }

        /**
         * Guarda las respuestas del onboarding y valida los datos más importantes.
         */
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
            validarTextoObligatorio(this.preferredName, "preferredName");
            validarTextoOpcional(this.preferredName, "preferredName", 2, 160);
            validarTextoObligatorio(this.language, "language");
            validarTextoOpcional(this.language, "language", 2, 16);
            validarTextoObligatorio(this.timezone, "timezone");
            validarTextoOpcional(this.timezone, "timezone", 2, 64);
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

    /**
     * Estado de ánimo elegido durante el onboarding.
     */
    public static class CurrentMoodRequest {
        @NotBlank(message = "label no puede estar vacio")
        @Size(max = 80, message = "label es demasiado largo")
        private String label;

        @Min(value = 1, message = "intensity esta fuera del rango permitido")
        @Max(value = 10, message = "intensity esta fuera del rango permitido")
        private int intensity;

        public CurrentMoodRequest() {
        }

        /**
         * Crea el estado de ánimo con una etiqueta y una intensidad.
         */
        public CurrentMoodRequest(String label, int intensity) {
            this.label = label;
            this.intensity = intensity;
            validarTextoObligatorio(this.label, "label");
            validarLongitud(this.label, "label", 80);
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

    /**
     * Contacto de confianza que el usuario puede dejar durante el onboarding.
     */
    public static class TrustedContactRequest {
        @Size(max = 160, message = "name es demasiado largo")
        private String name;

        @Size(max = 40, message = "phone es demasiado largo")
        private String phone;

        @Size(max = 80, message = "relationship es demasiado largo")
        private String relationship;

        public TrustedContactRequest() {
        }

        /**
         * Recoge los datos básicos del contacto y limita textos muy largos.
         */
        public TrustedContactRequest(String name, String phone, String relationship) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            validarLongitud(this.name, "name", 160);
            validarLongitud(this.phone, "phone", 40);
            validarLongitud(this.relationship, "relationship", 80);
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
