package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.PanicAlert;
import com.auraia.backend.models.entities.PanicNotificationResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PanicNotificationResultRepository extends JpaRepository<PanicNotificationResult, UUID> {

    List<PanicNotificationResult> findByAlert(PanicAlert alert);

    void deleteAllByAlert(PanicAlert alert);
}
