package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.OAuthState;
import com.auraia.backend.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthStateRepository extends JpaRepository<OAuthState, UUID> {

    Optional<OAuthState> findByStateHash(String stateHash);

    void deleteAllByUser(User user);
}
