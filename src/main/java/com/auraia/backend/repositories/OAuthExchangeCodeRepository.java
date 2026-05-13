package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.OAuthExchangeCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, UUID> {

    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);
}
