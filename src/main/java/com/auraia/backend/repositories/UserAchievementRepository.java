package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserAchievement;
import com.auraia.backend.models.enums.AchievementCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    List<UserAchievement> findByUser(User user);

    Optional<UserAchievement> findByUserAndCode(User user, AchievementCode code);

    List<UserAchievement> findByUserOrderByUnlockedAtAsc(User user);

    void deleteAllByUser(User user);
}
