package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUser(User user);

    void deleteAllByUser(User user);
}
