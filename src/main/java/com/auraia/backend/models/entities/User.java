package com.auraia.backend.models.entities;

import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Entity(name = "AppUser")
@Table(name = "users")
public class User extends AuditedEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column
    private String passwordHash;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Plan plan;

    @Column(nullable = false)
    private boolean emailVerified;

    private Instant onboardedAt;

    private Instant onboardingConsentAt;

    @Column(length = 120)
    private String onboardingConsentVersion;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> onboardingProfile = new LinkedHashMap<>();

    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
