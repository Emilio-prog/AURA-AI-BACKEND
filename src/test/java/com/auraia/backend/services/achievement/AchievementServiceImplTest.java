package com.auraia.backend.services.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.models.dto.request.AchievementRequests;
import com.auraia.backend.models.dto.response.AchievementResponses;
import com.auraia.backend.models.entities.AchievementEvent;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserAchievement;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.AchievementCode;
import com.auraia.backend.models.enums.AchievementEventType;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.AchievementEventRepository;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AchievementServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    DiaryEntryRepository diaryEntryRepository;
    @Mock
    MoodLogRepository moodLogRepository;
    @Mock
    ContactRepository contactRepository;
    @Mock
    ChatSessionRepository chatSessionRepository;
    @Mock
    UserAchievementRepository userAchievementRepository;
    @Mock
    AchievementEventRepository achievementEventRepository;

    AchievementServiceImpl service;
    User user;
    UUID userId;

    @BeforeEach
    void setUp() {
        service = new AchievementServiceImpl(
            userRepository,
            userSettingsRepository,
            diaryEntryRepository,
            moodLogRepository,
            contactRepository,
            chatSessionRepository,
            userAchievementRepository,
            achievementEventRepository
        );
        userId = UUID.randomUUID();
        user = User.builder()
            .email("emilio@example.com")
            .passwordHash("hash")
            .name("Emilio")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));

        UserSettings settings = UserSettings.builder()
            .user(user)
            .theme(Theme.SYSTEM)
            .language("es")
            .timezone("Europe/Madrid")
            .build();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        lenient().when(userSettingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        lenient().when(userAchievementRepository.findByUser(user)).thenReturn(List.of());
        lenient().when(diaryEntryRepository.findCreatedAtByUser(user)).thenReturn(List.of());
        lenient().when(moodLogRepository.findLoggedAtByUser(user)).thenReturn(List.of());
        lenient().when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUnlocksRetroactiveBackendAchievements() {
        user.setOnboardedAt(Instant.parse("2026-05-10T12:00:00Z"));
        when(chatSessionRepository.countByUser(user)).thenReturn(1L);
        when(diaryEntryRepository.countByUser(user)).thenReturn(8L);
        when(diaryEntryRepository.findCreatedAtByUser(user)).thenReturn(List.of(
            Instant.parse("2026-05-01T10:00:00Z"),
            Instant.parse("2026-05-02T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-04T10:00:00Z"),
            Instant.parse("2026-05-05T10:00:00Z"),
            Instant.parse("2026-05-06T10:00:00Z"),
            Instant.parse("2026-05-07T10:00:00Z")
        ));
        when(moodLogRepository.countByUser(user)).thenReturn(3L);
        when(moodLogRepository.findLoggedAtByUser(user)).thenReturn(List.of(
            Instant.parse("2026-05-01T09:00:00Z"),
            Instant.parse("2026-05-02T09:00:00Z"),
            Instant.parse("2026-05-03T09:00:00Z")
        ));
        when(contactRepository.countByUserAndSosEnabledTrueAndAvailableTrue(user)).thenReturn(1L);

        AchievementResponses.AchievementListResponse response = service.list();

        assertThat(response.total()).isEqualTo(8);
        assertThat(response.unlocked()).isEqualTo(7);
        assertThat(response.achievements())
            .filteredOn(AchievementResponses.AchievementResponse::unlocked)
            .extracting(AchievementResponses.AchievementResponse::code)
            .contains(
                AchievementCode.REFUGIO_ACTIVADO,
                AchievementCode.PRIMER_CHAT_AURA,
                AchievementCode.PRIMERA_ENTRADA_DIARIO,
                AchievementCode.SIETE_DIAS_DIARIO,
                AchievementCode.PRIMER_CHECKIN_MOOD,
                AchievementCode.TRES_DIAS_MOOD,
                AchievementCode.RED_SOS_ACTIVA
            )
            .doesNotContain(AchievementCode.EXPLORADOR_CALMA);
    }

    @Test
    void recordEventIsIdempotentByUserAndKey() {
        when(achievementEventRepository.findByUserAndIdempotencyKey(user, "BREATHING_COMPLETED:2026-05-11"))
            .thenReturn(Optional.of(AchievementEvent.builder()
                .user(user)
                .eventType(AchievementEventType.BREATHING_COMPLETED)
                .idempotencyKey("BREATHING_COMPLETED:2026-05-11")
                .occurredAt(Instant.parse("2026-05-11T20:00:00Z"))
                .metadata(Map.of())
                .build()));

        service.recordEvent(new AchievementRequests.EventRequest(
            AchievementEventType.BREATHING_COMPLETED,
            "BREATHING_COMPLETED:2026-05-11",
            Instant.parse("2026-05-11T20:00:00Z"),
            Map.of()
        ));

        verify(achievementEventRepository, never()).save(any(AchievementEvent.class));
    }

    @Test
    void exploradorCalmaUnlocksOnlyWhenThreeEventsExist() {
        when(achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.BREATHING_COMPLETED)).thenReturn(true);
        when(achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.SOUNDSCAPE_PLAYED)).thenReturn(true);
        when(achievementEventRepository.existsByUserAndEventType(user, AchievementEventType.MINIGAME_OPENED)).thenReturn(true);

        AchievementResponses.AchievementListResponse response = service.list();

        assertThat(response.achievements())
            .filteredOn(achievement -> achievement.code() == AchievementCode.EXPLORADOR_CALMA)
            .singleElement()
            .satisfies(achievement -> {
                assertThat(achievement.progress()).isEqualTo(3);
                assertThat(achievement.unlocked()).isTrue();
            });
    }

    @Test
    void recalculatingDoesNotDuplicateAlreadyUnlockedAchievement() {
        user.setOnboardedAt(Instant.parse("2026-05-10T12:00:00Z"));
        UserAchievement existing = UserAchievement.builder()
            .user(user)
            .code(AchievementCode.REFUGIO_ACTIVADO)
            .unlockedAt(Instant.parse("2026-05-10T12:00:00Z"))
            .progressSnapshot(Map.of("progress", 1, "target", 1))
            .build();
        when(userAchievementRepository.findByUser(user)).thenReturn(List.of(existing));

        service.list();

        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }
}
