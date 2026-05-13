package com.auraia.backend.services.auth;

public interface SupabaseAuthClient {

    SupabaseAuthUser fetchUser(String accessToken);
}
