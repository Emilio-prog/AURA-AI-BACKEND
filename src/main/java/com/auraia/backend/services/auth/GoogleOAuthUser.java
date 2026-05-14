package com.auraia.backend.services.auth;

public record GoogleOAuthUser(
    String subject,
    String email,
    boolean emailVerified,
    String name
) {
}
