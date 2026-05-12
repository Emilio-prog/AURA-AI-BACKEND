package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.WebPushSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, UUID> {

    Optional<WebPushSubscription> findByUserAndEndpointHash(User user, String endpointHash);

    List<WebPushSubscription> findByUserAndActiveTrue(User user);

    List<WebPushSubscription> findByActiveTrue();

    boolean existsByUserAndActiveTrue(User user);

    void deleteAllByUser(User user);
}
