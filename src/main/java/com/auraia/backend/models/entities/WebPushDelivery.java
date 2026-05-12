package com.auraia.backend.models.entities;

import com.auraia.backend.models.enums.WebPushDeliveryStatus;
import com.auraia.backend.models.enums.WebPushNotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "web_push_deliveries")
public class WebPushDelivery extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WebPushSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private WebPushNotificationType notificationType;

    @Column(nullable = false, length = 160)
    private String targetKey;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WebPushDeliveryStatus status;

    private Integer providerStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant sentAt;
}
