package com.auraia.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "email_suppressions")
public class EmailSuppression extends CreatedEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 64)
    private String reason;
}
