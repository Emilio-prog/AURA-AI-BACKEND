package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class DefaultSupabaseAuthClient implements SupabaseAuthClient {

    private final AppProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    @PostConstruct
    void initialize() {
        AppProperties.SupabaseAuth config = properties.getSupabaseAuth();
        if (!config.isEnabled()) {
            return;
        }
        if (isBlank(config.getUrl()) || isBlank(config.getAnonKey())) {
            throw new IllegalStateException("SUPABASE_URL and SUPABASE_ANON_KEY are required when SUPABASE_AUTH_ENABLED=true");
        }
    }

    @Override
    public SupabaseAuthUser fetchUser(String accessToken) {
        ensureEnabled();
        AppProperties.SupabaseAuth config = properties.getSupabaseAuth();
        SupabaseUserResponse user;
        try {
            user = restClient.get()
                .uri(normalizedUrl(config.getUrl()) + "/auth/v1/user")
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.set("apikey", config.getAnonKey());
                })
                .retrieve()
                .body(SupabaseUserResponse.class);
        } catch (RestClientException ex) {
            throw new BusinessException("error.supabase_auth_failed");
        }

        if (user == null || isBlank(user.id()) || isBlank(user.email())) {
            throw new BusinessException("error.supabase_auth_failed");
        }

        return new SupabaseAuthUser(
            user.id(),
            user.email(),
            user.emailConfirmedAt() != null || metadataBoolean(user.userMetadata(), "email_verified"),
            displayName(user.userMetadata())
        );
    }

    private void ensureEnabled() {
        if (!properties.getSupabaseAuth().isEnabled()) {
            throw new BusinessException("error.supabase_auth_unavailable");
        }
    }

    private String normalizedUrl(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String displayName(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        return firstText(metadata, "full_name", "name", "preferred_name");
    }

    private String firstText(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private boolean metadataBoolean(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return false;
        }
        return Boolean.TRUE.equals(metadata.get(key));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SupabaseUserResponse(
        String id,
        String email,
        @JsonProperty("email_confirmed_at") String emailConfirmedAt,
        @JsonProperty("user_metadata") Map<String, Object> userMetadata
    ) {
    }
}
