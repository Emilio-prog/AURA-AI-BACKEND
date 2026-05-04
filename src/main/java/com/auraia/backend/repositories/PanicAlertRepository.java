package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.PanicAlert;
import com.auraia.backend.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PanicAlertRepository extends JpaRepository<PanicAlert, UUID> {

    Page<PanicAlert> findByUser(User user, Pageable pageable);

    Optional<PanicAlert> findByIdAndUser(UUID id, User user);

    void deleteAllByUser(User user);
}
