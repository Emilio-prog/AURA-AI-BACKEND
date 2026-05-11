package com.auraia.backend.models.entities;

import com.auraia.backend.models.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class UserSubscription extends AuditedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 120, unique = true)
    private String stripeCustomerId;

    @Column(length = 120, unique = true)
    private String stripeSubscriptionId;

    @Column(length = 120)
    private String stripePriceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Plan plan;

    @Column(nullable = false, length = 40)
    private String status;

    private Instant currentPeriodEnd;

    @Column(nullable = false)
    private boolean cancelAtPeriodEnd;

    private Instant canceledAt;
}
