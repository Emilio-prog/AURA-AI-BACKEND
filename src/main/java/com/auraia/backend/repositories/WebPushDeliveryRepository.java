package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.WebPushDelivery;
import com.auraia.backend.models.entities.WebPushSubscription;
import com.auraia.backend.models.enums.WebPushNotificationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebPushDeliveryRepository extends JpaRepository<WebPushDelivery, UUID> {

    Optional<WebPushDelivery> findByUserAndSubscriptionAndNotificationTypeAndTargetKey(
        User user,
        WebPushSubscription subscription,
        WebPushNotificationType notificationType,
        String targetKey
    );

    boolean existsByUserAndSubscriptionAndNotificationTypeAndTargetKey(
        User user,
        WebPushSubscription subscription,
        WebPushNotificationType notificationType,
        String targetKey
    );

    List<WebPushDelivery> findByUserAndNotificationType(User user, WebPushNotificationType notificationType);

    void deleteAllByUser(User user);
}
