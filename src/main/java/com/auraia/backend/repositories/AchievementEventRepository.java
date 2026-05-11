package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.AchievementEvent;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.enums.AchievementEventType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementEventRepository extends JpaRepository<AchievementEvent, UUID> {

    Optional<AchievementEvent> findByUserAndIdempotencyKey(User user, String idempotencyKey);

    boolean existsByUserAndEventType(User user, AchievementEventType eventType);

    void deleteAllByUser(User user);
}
