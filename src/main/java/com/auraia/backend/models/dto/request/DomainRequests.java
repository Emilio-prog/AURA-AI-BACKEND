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

/**
 * Peticiones de las pantallas principales de la aplicación.
 * Incluye diario, ánimo, chat, contactos y ajustes.
 */
public final class DomainRequests {

    private DomainRequests() {
    }

    /**
     * Comprueba textos obligatorios antes de crear la petición.
     */
    private static void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
    }

    /**
     * Evita que lleguen textos demasiado grandes.
     */
    private static void validarLongitud(String valor, String campo, int maximo) {
        if (valor != null && valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " es demasiado largo");
        }
    }

    /**
     * Datos que se mandan al crear o editar una entrada del diario.
     */
    public static class DiaryEntryRequest {
        @Size(max = 180, message = "title es demasiado largo")
        private String title;

        @NotBlank(message = "content no puede estar vacio")
        @Size(max = 20000, message = "content es demasiado largo")
        private String content;

        @Min(value = 1, message = "moodScore esta fuera del rango permitido")
        @Max(value = 10, message = "moodScore esta fuera del rango permitido")
        private Integer moodScore;

        @Size(max = 80, message = "moodLabel es demasiado largo")
        private String moodLabel;

        private List<@Size(min = 0, max = 80, message = "tags tiene una longitud incorrecta") String> tags;

        public DiaryEntryRequest() {
        }

        /**
         * Valida el contenido del diario y limita campos largos como título o estado.
         */
        public DiaryEntryRequest(String title, String content, Integer moodScore, String moodLabel, List<String> tags) {
            this.title = title;
            this.content = content;
            this.moodScore = moodScore;
            this.moodLabel = moodLabel;
            this.tags = tags;
            validarLongitud(this.title, "title", 180);
            validarTextoObligatorio(this.content, "content");
            validarLongitud(this.content, "content", 20000);
            validarLongitud(this.moodLabel, "moodLabel", 80);
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

    /**
     * Registro de ánimo antes y después de usar Aura.
     */
    public static class MoodLogRequest {
        @Min(value = 1, message = "beforeLevel esta fuera del rango permitido")
        @Max(value = 10, message = "beforeLevel esta fuera del rango permitido")
        private int beforeLevel;

        @Min(value = 1, message = "afterLevel esta fuera del rango permitido")
        @Max(value = 10, message = "afterLevel esta fuera del rango permitido")
        private int afterLevel;

        @Size(max = 2000, message = "note es demasiado largo")
        private String note;

        private Instant loggedAt;

        public MoodLogRequest() {
        }

        /**
         * Crea una sesión de ánimo con nota opcional y fecha opcional.
         */
        public MoodLogRequest(int beforeLevel, int afterLevel, String note, Instant loggedAt) {
            this.beforeLevel = beforeLevel;
            this.afterLevel = afterLevel;
            this.note = note;
            this.loggedAt = loggedAt;
            validarLongitud(this.note, "note", 2000);
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

    /**
     * Mensaje que el usuario escribe para hablar con Aura IA.
     */
    public static class ChatMessageRequest {
        @NotBlank(message = "message no puede estar vacio")
        @Size(max = 8000, message = "message es demasiado largo")
        private String message;

        public ChatMessageRequest() {
        }

        /**
         * Revisa que el mensaje no esté vacío ni sea demasiado largo.
         */
        public ChatMessageRequest(String message) {
            this.message = message;
            validarTextoObligatorio(this.message, "message");
            validarLongitud(this.message, "message", 8000);
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Datos de un contacto de confianza del usuario.
     */
    public static class ContactRequest {
        @NotBlank(message = "name no puede estar vacio")
        @Size(max = 160, message = "name es demasiado largo")
        private String name;

        @NotBlank(message = "phone no puede estar vacio")
        @Size(max = 40, message = "phone es demasiado largo")
        private String phone;

        @Size(max = 80, message = "relationship es demasiado largo")
        private String relationship;

        @Min(value = 1, message = "priority esta fuera del rango permitido")
        @Max(value = 99, message = "priority esta fuera del rango permitido")
        private Integer priority;

        private Boolean available;
        private Boolean sosEnabled;

        public ContactRequest() {
        }

        /**
         * Crea el contacto y comprueba los campos básicos.
         */
        public ContactRequest(String name, String phone, String relationship, Integer priority, Boolean available, Boolean sosEnabled) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            this.priority = priority;
            this.available = available;
            this.sosEnabled = sosEnabled;
            validarTextoObligatorio(this.name, "name");
            validarLongitud(this.name, "name", 160);
            validarTextoObligatorio(this.phone, "phone");
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
        @Size(max = 3000, message = "notes es demasiado largo")
        private String notes;

        private UUID contactId;
        private Map<String, Object> contextJson;

        public PanicTriggerRequest() {
        }

        public PanicTriggerRequest(String notes, UUID contactId, Map<String, Object> contextJson) {
            this.notes = notes;
            this.contactId = contactId;
            this.contextJson = contextJson;
            validarLongitud(this.notes, "notes", 3000);
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
        @Size(max = 3000, message = "notes es demasiado largo")
        private String notes;

        public PanicResolveRequest() {
        }

        public PanicResolveRequest(String notes) {
            this.notes = notes;
            validarLongitud(this.notes, "notes", 3000);
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Ajustes del usuario, como tema, idioma y notificaciones.
     */
    public static class UserSettingsRequest {
        private Theme theme;

        @Size(min = 2, max = 16, message = "language tiene una longitud incorrecta")
        private String language;

        @Size(min = 2, max = 64, message = "timezone tiene una longitud incorrecta")
        private String timezone;

        @NotNull(message = "notificationPreferences no puede estar vacio")
        private Map<String, Object> notificationPreferences;

        public UserSettingsRequest() {
        }

        /**
         * Guarda los ajustes recibidos desde la pantalla de configuración.
         */
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
