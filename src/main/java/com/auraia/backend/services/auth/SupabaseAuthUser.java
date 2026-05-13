package com.auraia.backend.services.auth;

public record SupabaseAuthUser(
    String subject,
    String email,
    boolean emailVerified,
    String name
) {
}
