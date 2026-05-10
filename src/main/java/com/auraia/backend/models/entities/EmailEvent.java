package com.auraia.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "email_events")
public class EmailEvent extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 320)
    private String recipientEmail;

    @Column(length = 64)
    private String resendEmailId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant receivedAt;
}
