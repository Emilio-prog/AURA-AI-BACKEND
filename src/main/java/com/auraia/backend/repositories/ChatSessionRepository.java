package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.ChatSession;
import com.auraia.backend.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Page<ChatSession> findByUser(User user, Pageable pageable);

    Optional<ChatSession> findByIdAndUser(UUID id, User user);

    long countByUser(User user);

    void deleteAllByUser(User user);
}
