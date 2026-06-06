package com.auraia.backend.services.achievement;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.AchievementRequests;
import com.auraia.backend.models.dto.response.AchievementResponses;
import com.auraia.backend.models.entities.AchievementEvent;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserAchievement;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.AchievementCode;
import com.auraia.backend.models.enums.AchievementEventType;
import com.auraia.backend.repositories.AchievementEventRepository;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.SecurityUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Europe/Madrid");
    private static final List<Definition> CATALOG = List.of(
        new Definition(AchievementCode.REFUGIO_ACTIVADO, "Refugio activado", "Completa el onboarding inicial.", "Inicio", "#2DD4BF", 1),
        new Definition(AchievementCode.PRIMER_CHAT_AURA, "Primer chat con AURA", "Abre una primera conversacion con AURA.", "AURA", "#A855F7", 1),
        new Definition(AchievementCode.PRIMERA_ENTRADA_DIARIO, "Primera entrada de diario", "Guarda tu primera nota en el diario.", "Diario", "#FB7185", 1),
        new Definition(AchievementCode.SIETE_DIAS_DIARIO, "7 dias de diario", "Escribe en el diario durante 7 dias distintos.", "Diario", "#FB7185", 7),
        new Definition(AchievementCode.PRIMER_CHECKIN_MOOD, "Primer check-in emocional", "Registra tu estado de animo una vez.", "Mood", "#2DD4BF", 1),
        new Definition(AchievementCode.TRES_DIAS_MOOD, "3 dias de mood", "Registra tu mood en 3 dias distintos.", "Mood", "#2DD4BF", 3),
        new Definition(AchievementCode.RED_SOS_ACTIVA, "Red SOS activa", "Activa al menos un contacto SOS disponible.", "SOS", "#FB7185", 1),
        new Definition(AchievementCode.EXPLORADOR_CALMA, "Explorador de calma", "Completa respiracion, sonido y minijuego.", "Calma", "#A855F7", 3)
    );

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final MoodLogRepository moodLogRepository;
    private final ContactRepository contactRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementEventRepository achievementEventRepository;

    @Override
    @Transactional
    public AchievementResponses.AchievementListResponse list() {
        return evaluateAndRespond(currentUser());
    }

    @Override
    @Transactional
    public AchievementResponses.AchievementListResponse recordEvent(AchievementRequests.EventRequest request) {
        User user = currentUser();
        String idempotencyKey = request.getIdempotencyKey().trim();
        if (achievementEventRepository.findByUserAndIdempotencyKey(user, idempotencyKey).isEmpty()) {
            achievementEventRepository.save(AchievementEvent.builder()
                .user(user)
                .eventType(request.getType())
                .idempotencyKey(idempotencyKey)
                .occurredAt(request.getOccurredAt() == null ? Instant.now() : request.getOccurredAt())
                .metadata(request.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getMetadata()))
                .build());
        }
        return evaluateAndRespond(user);
    }

    private AchievementResponses.AchievementListResponse evaluateAndRespond(User user) {
        ZoneId zone = userZone(user);
        Metrics metrics = new Metrics(
            user.getOnboardedAt() == null ? 0 : 1,
            safeLongToInt(chatSessionRepository.countByUser(user)),
            safeLongToInt(diaryEntryRepository.countByUser(user)),
            distinctDays(diaryEntryRepository.findCreatedAtByUser(user), zone),
            safeLongToInt(moodLogRepository.countByUser(user)),
            distinctDays(moodLogRepository.findLoggedAtByUser(user), zone),
            safeLongToInt(contactRepository.countByUserAndSosEnabledTrueAndAvailableTrue(user)),
            achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.BREATHING_COMPLETED),
            achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.SOUNDSCAPE_PLAYED),
            achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.MINIGAME_OPENED)
        );

        Map<AchievementCode, UserAchievement> unlocked = userAchievementRepository.findByUser(user).stream()
            .collect(Collectors.toMap(UserAchievement::getCode, Function.identity(), (left, right) -> left));

        List<AchievementResponses.AchievementResponse> achievements = CATALOG.stream()
            .map(definition -> responseFor(user, definition, metrics, unlocked))
            .toList();
        int unlockedCount = (int) achievements.stream().filter(AchievementResponses.AchievementResponse::isUnlocked).count();
        return new AchievementResponses.AchievementListResponse(achievements.size(), unlockedCount, achievements);
    }

    private AchievementResponses.AchievementResponse responseFor(User user,
                                                                 Definition definition,
                                                                 Metrics metrics,
                                                                 Map<AchievementCode, UserAchievement> unlocked) {
        int progress = Math.min(progress(definition.getCode(), metrics), definition.getTarget());
        UserAchievement achievement = unlocked.get(definition.getCode());
        if (achievement == null && progress >= definition.getTarget()) {
            achievement = userAchievementRepository.save(UserAchievement.builder()
                .user(user)
                .code(definition.getCode())
                .unlockedAt(Instant.now())
                .progressSnapshot(snapshot(definition, progress))
                .build());
            unlocked.put(definition.getCode(), achievement);
        }
        boolean isUnlocked = achievement != null;
        return new AchievementResponses.AchievementResponse(
            definition.getCode(),
            definition.getTitle(),
            definition.getDescription(),
            definition.getCategory(),
            definition.getAccent(),
            progress,
            definition.getTarget(),
            isUnlocked,
            isUnlocked ? achievement.getUnlockedAt() : null,
            progress + "/" + definition.getTarget()
        );
    }

    private int progress(AchievementCode code, Metrics metrics) {
        return switch (code) {
            case REFUGIO_ACTIVADO -> metrics.getOnboardingCompleted();
            case PRIMER_CHAT_AURA -> metrics.getChatSessions();
            case PRIMERA_ENTRADA_DIARIO -> metrics.getDiaryEntries();
            case SIETE_DIAS_DIARIO -> metrics.getDiaryDays();
            case PRIMER_CHECKIN_MOOD -> metrics.getMoodLogs();
            case TRES_DIAS_MOOD -> metrics.getMoodDays();
            case RED_SOS_ACTIVA -> metrics.getActiveSosContacts();
            case EXPLORADOR_CALMA -> calmExplorerProgress(metrics);
        };
    }

    private int calmExplorerProgress(Metrics metrics) {
        int progress = 0;
        if (metrics.isBreathingCompleted()) {
            progress++;
        }
        if (metrics.isSoundscapePlayed()) {
            progress++;
        }
        if (metrics.isMinigameOpened()) {
            progress++;
        }
        return progress;
    }

    private Map<String, Object> snapshot(Definition definition, int progress) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("progress", progress);
        snapshot.put("target", definition.getTarget());
        snapshot.put("evaluatedAt", Instant.now().toString());
        return snapshot;
    }

    private int distinctDays(List<Instant> instants, ZoneId zone) {
        return (int) instants.stream()
            .map(instant -> instant.atZone(zone).toLocalDate())
            .distinct()
            .count();
    }

    private ZoneId userZone(User user) {
        String timezone = userSettingsRepository.findByUser(user)
            .map(UserSettings::getTimezone)
            .filter(value -> !value.isBlank())
            .orElse(FALLBACK_ZONE.getId());
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return FALLBACK_ZONE;
        }
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private int safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static class Definition {
        private AchievementCode code;
        private String title;
        private String description;
        private String category;
        private String accent;
        private int target;

        private Definition() {
        }

        private Definition(AchievementCode code, String title, String description,
                           String category, String accent, int target) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.category = category;
            this.accent = accent;
            this.target = target;
        }

        private AchievementCode getCode() {
            return code;
        }

        private void setCode(AchievementCode code) {
            this.code = code;
        }

        private String getTitle() {
            return title;
        }

        private void setTitle(String title) {
            this.title = title;
        }

        private String getDescription() {
            return description;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        private String getCategory() {
            return category;
        }

        private void setCategory(String category) {
            this.category = category;
        }

        private String getAccent() {
            return accent;
        }

        private void setAccent(String accent) {
            this.accent = accent;
        }

        private int getTarget() {
            return target;
        }

        private void setTarget(int target) {
            this.target = target;
        }
    }

    private static class Metrics {
        private int onboardingCompleted;
        private int chatSessions;
        private int diaryEntries;
        private int diaryDays;
        private int moodLogs;
        private int moodDays;
        private int activeSosContacts;
        private boolean breathingCompleted;
        private boolean soundscapePlayed;
        private boolean minigameOpened;

        private Metrics() {
        }

        private Metrics(int onboardingCompleted, int chatSessions, int diaryEntries,
                        int diaryDays, int moodLogs, int moodDays, int activeSosContacts,
                        boolean breathingCompleted, boolean soundscapePlayed,
                        boolean minigameOpened) {
            this.onboardingCompleted = onboardingCompleted;
            this.chatSessions = chatSessions;
            this.diaryEntries = diaryEntries;
            this.diaryDays = diaryDays;
            this.moodLogs = moodLogs;
            this.moodDays = moodDays;
            this.activeSosContacts = activeSosContacts;
            this.breathingCompleted = breathingCompleted;
            this.soundscapePlayed = soundscapePlayed;
            this.minigameOpened = minigameOpened;
        }

        private int getOnboardingCompleted() {
            return onboardingCompleted;
        }

        private void setOnboardingCompleted(int onboardingCompleted) {
            this.onboardingCompleted = onboardingCompleted;
        }

        private int getChatSessions() {
            return chatSessions;
        }

        private void setChatSessions(int chatSessions) {
            this.chatSessions = chatSessions;
        }

        private int getDiaryEntries() {
            return diaryEntries;
        }

        private void setDiaryEntries(int diaryEntries) {
            this.diaryEntries = diaryEntries;
        }

        private int getDiaryDays() {
            return diaryDays;
        }

        private void setDiaryDays(int diaryDays) {
            this.diaryDays = diaryDays;
        }

        private int getMoodLogs() {
            return moodLogs;
        }

        private void setMoodLogs(int moodLogs) {
            this.moodLogs = moodLogs;
        }

        private int getMoodDays() {
            return moodDays;
        }

        private void setMoodDays(int moodDays) {
            this.moodDays = moodDays;
        }

        private int getActiveSosContacts() {
            return activeSosContacts;
        }

        private void setActiveSosContacts(int activeSosContacts) {
            this.activeSosContacts = activeSosContacts;
        }

        private boolean isBreathingCompleted() {
            return breathingCompleted;
        }

        private void setBreathingCompleted(boolean breathingCompleted) {
            this.breathingCompleted = breathingCompleted;
        }

        private boolean isSoundscapePlayed() {
            return soundscapePlayed;
        }

        private void setSoundscapePlayed(boolean soundscapePlayed) {
            this.soundscapePlayed = soundscapePlayed;
        }

        private boolean isMinigameOpened() {
            return minigameOpened;
        }

        private void setMinigameOpened(boolean minigameOpened) {
            this.minigameOpened = minigameOpened;
        }
    }
}
