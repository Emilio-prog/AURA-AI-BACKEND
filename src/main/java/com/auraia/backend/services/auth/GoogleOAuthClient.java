package com.auraia.backend.services.auth;

public interface GoogleOAuthClient {

    String authorizationUrl(String state);

    GoogleOAuthUser fetchUser(String code);
}
