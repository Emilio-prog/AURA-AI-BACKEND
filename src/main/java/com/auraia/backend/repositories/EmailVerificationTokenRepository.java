package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.EmailVerificationToken;
import com.auraia.backend.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);
}
