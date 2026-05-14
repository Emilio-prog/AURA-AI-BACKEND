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
        String idempotencyKey = request.idempotencyKey().trim();
        if (achievementEventRepository.findByUserAndIdempotencyKey(user, idempotencyKey).isEmpty()) {
            achievementEventRepository.save(AchievementEvent.builder()
                .user(user)
                .eventType(request.type())
                .idempotencyKey(idempotencyKey)
                .occurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt())
                .metadata(request.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.metadata()))
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
        int unlockedCount = (int) achievements.stream().filter(AchievementResponses.AchievementResponse::unlocked).count();
        return new AchievementResponses.AchievementListResponse(achievements.size(), unlockedCount, achievements);
    }

    private AchievementResponses.AchievementResponse responseFor(User user,
                                                                 Definition definition,
                                                                 Metrics metrics,
                                                                 Map<AchievementCode, UserAchievement> unlocked) {
        int progress = Math.min(progress(definition.code(), metrics), definition.target());
        UserAchievement achievement = unlocked.get(definition.code());
        if (achievement == null && progress >= definition.target()) {
            achievement = userAchievementRepository.save(UserAchievement.builder()
                .user(user)
                .code(definition.code())
                .unlockedAt(Instant.now())
                .progressSnapshot(snapshot(definition, progress))
                .build());
            unlocked.put(definition.code(), achievement);
        }
        boolean isUnlocked = achievement != null;
        return new AchievementResponses.AchievementResponse(
            definition.code(),
            definition.title(),
            definition.description(),
            definition.category(),
            definition.accent(),
            progress,
            definition.target(),
            isUnlocked,
            isUnlocked ? achievement.getUnlockedAt() : null,
            progress + "/" + definition.target()
        );
    }

    private int progress(AchievementCode code, Metrics metrics) {
        return switch (code) {
            case REFUGIO_ACTIVADO -> metrics.onboardingCompleted();
            case PRIMER_CHAT_AURA -> metrics.chatSessions();
            case PRIMERA_ENTRADA_DIARIO -> metrics.diaryEntries();
            case SIETE_DIAS_DIARIO -> metrics.diaryDays();
            case PRIMER_CHECKIN_MOOD -> metrics.moodLogs();
            case TRES_DIAS_MOOD -> metrics.moodDays();
            case RED_SOS_ACTIVA -> metrics.activeSosContacts();
            case EXPLORADOR_CALMA -> calmExplorerProgress(metrics);
        };
    }

    private int calmExplorerProgress(Metrics metrics) {
        int progress = 0;
        if (metrics.breathingCompleted()) {
            progress++;
        }
        if (metrics.soundscapePlayed()) {
            progress++;
        }
        if (metrics.minigameOpened()) {
            progress++;
        }
        return progress;
    }

    private Map<String, Object> snapshot(Definition definition, int progress) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("progress", progress);
        snapshot.put("target", definition.target());
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

    private record Definition(
        AchievementCode code,
        String title,
        String description,
        String category,
        String accent,
        int target
    ) {
    }

    private record Metrics(
        int onboardingCompleted,
        int chatSessions,
        int diaryEntries,
        int diaryDays,
        int moodLogs,
        int moodDays,
        int activeSosContacts,
        boolean breathingCompleted,
        boolean soundscapePlayed,
        boolean minigameOpened
    ) {
    }
}
