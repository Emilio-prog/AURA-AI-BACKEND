package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.EmailSuppression;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSuppressionRepository extends JpaRepository<EmailSuppression, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<EmailSuppression> findByEmailIgnoreCase(String email);
}
