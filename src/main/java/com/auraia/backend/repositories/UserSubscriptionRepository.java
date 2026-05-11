package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSubscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUser(User user);

    Optional<UserSubscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
