package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.MoodLog;
import com.auraia.backend.models.entities.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MoodLogRepository extends JpaRepository<MoodLog, UUID> {

    Page<MoodLog> findByUserAndLoggedAtBetween(User user, Instant start, Instant end, Pageable pageable);

    List<MoodLog> findByUserAndLoggedAtBetweenOrderByLoggedAtAsc(User user, Instant start, Instant end);

    Optional<MoodLog> findByIdAndUser(UUID id, User user);

    long countByUser(User user);

    @Query("select m.loggedAt from MoodLog m where m.user = :user")
    List<Instant> findLoggedAtByUser(User user);

    void deleteAllByUser(User user);
}
