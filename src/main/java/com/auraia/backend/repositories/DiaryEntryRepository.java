package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.DiaryEntry;
import com.auraia.backend.models.entities.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, UUID> {

    Page<DiaryEntry> findByUser(User user, Pageable pageable);

    Page<DiaryEntry> findByUserAndCreatedAtBetween(User user, Instant start, Instant end, Pageable pageable);

    Optional<DiaryEntry> findByIdAndUser(UUID id, User user);

    long countByUser(User user);

    @Query("select d.createdAt from DiaryEntry d where d.user = :user")
    List<Instant> findCreatedAtByUser(User user);

    void deleteAllByUser(User user);
}
