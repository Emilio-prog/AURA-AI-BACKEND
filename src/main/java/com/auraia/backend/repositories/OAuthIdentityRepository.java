package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.OAuthIdentity;
import com.auraia.backend.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    Optional<OAuthIdentity> findByProviderAndProviderSubjectAndActiveTrue(String provider, String providerSubject);

    Optional<OAuthIdentity> findByUserAndProviderAndActiveTrue(User user, String provider);
}
