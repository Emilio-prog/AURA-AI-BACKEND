package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.EmailEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailEventRepository extends JpaRepository<EmailEvent, UUID> {
}
