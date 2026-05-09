package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.PasswordResetToken;
import com.auraia.backend.models.entities.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.consumedAt = :now WHERE t.user = :user AND t.consumedAt IS NULL")
    void invalidateAllActiveByUser(@Param("user") User user, @Param("now") Instant now);

    void deleteAllByUser(User user);
}
