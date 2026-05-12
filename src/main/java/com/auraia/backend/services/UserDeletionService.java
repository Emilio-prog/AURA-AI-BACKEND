package com.auraia.backend.services;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.AchievementEventRepository;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.EmailVerificationTokenRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.PanicAlertRepository;
import com.auraia.backend.repositories.RefreshTokenRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.repositories.WebPushDeliveryRepository;
import com.auraia.backend.repositories.WebPushSubscriptionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final MoodLogRepository moodLogRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ContactRepository contactRepository;
    private final PanicAlertRepository panicAlertRepository;
    private final AchievementEventRepository achievementEventRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final WebPushDeliveryRepository webPushDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    public void anonymizeAndSoftDelete(User user) {
        refreshTokenRepository.deleteAllByUser(user);
        emailVerificationTokenRepository.deleteAllByUser(user);
        userSettingsRepository.deleteAllByUser(user);
        diaryEntryRepository.deleteAllByUser(user);
        moodLogRepository.deleteAllByUser(user);
        chatSessionRepository.deleteAllByUser(user);
        contactRepository.deleteAllByUser(user);
        panicAlertRepository.deleteAllByUser(user);
        achievementEventRepository.deleteAllByUser(user);
        userAchievementRepository.deleteAllByUser(user);
        webPushDeliveryRepository.deleteAllByUser(user);
        webPushSubscriptionRepository.deleteAllByUser(user);

        user.setDeletedAt(Instant.now());
        user.setEmail("deleted-" + user.getId() + "@deleted.local");
        user.setName("Deleted User");
        user.setPasswordHash("{noop}deleted");
        user.setEmailVerified(false);
        userRepository.save(user);
    }
}
