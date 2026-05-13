package com.auraia.backend.services;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.AchievementEventRepository;
import com.auraia.backend.repositories.ChatSessionRepository;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.DiaryEntryRepository;
import com.auraia.backend.repositories.EmailVerificationTokenRepository;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.OAuthExchangeCodeRepository;
import com.auraia.backend.repositories.OAuthIdentityRepository;
import com.auraia.backend.repositories.OAuthStateRepository;
import com.auraia.backend.repositories.PanicAlertRepository;
import com.auraia.backend.repositories.PasswordResetTokenRepository;
import com.auraia.backend.repositories.RefreshTokenRepository;
import com.auraia.backend.repositories.UserAchievementRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.repositories.UserSubscriptionRepository;
import com.auraia.backend.repositories.WebPushDeliveryRepository;
import com.auraia.backend.repositories.WebPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
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
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final OAuthStateRepository oAuthStateRepository;
    private final OAuthExchangeCodeRepository oAuthExchangeCodeRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Transactional
    public void deletePermanently(User user) {
        refreshTokenRepository.deleteAllByUser(user);
        emailVerificationTokenRepository.deleteAllByUser(user);
        passwordResetTokenRepository.deleteAllByUser(user);
        oAuthExchangeCodeRepository.deleteAllByUser(user);
        oAuthStateRepository.deleteAllByUser(user);
        oAuthIdentityRepository.deleteAllByUser(user);
        userSubscriptionRepository.deleteAllByUser(user);
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
        userRepository.delete(user);
    }
}
