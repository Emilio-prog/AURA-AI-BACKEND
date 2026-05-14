package com.auraia.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@MappedSuperclass
public abstract class CreatedEntity extends BaseEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
