package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        AppProperties.Turnstile cfg = properties.getTurnstile();
        return cfg.isEnabled() && cfg.getSecretKey() != null && !cfg.getSecretKey().isBlank();
    }

    /**
     * Verifies a Turnstile token against Cloudflare's siteverify endpoint.
     * Returns true if Turnstile is disabled (no-op) or if the token is valid.
     * Returns false only when Turnstile is enabled and the token is missing or rejected.
     */
    public boolean verify(String token, String remoteIp) {
        if (!isEnabled()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            log.warn("Turnstile token missing on protected endpoint");
            return false;
        }

        AppProperties.Turnstile cfg = properties.getTurnstile();
        Map<String, String> form = new HashMap<>();
        form.put("secret", cfg.getSecretKey());
        form.put("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.put("remoteip", remoteIp);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cfg.getTimeoutMs()))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VERIFY_URL))
                .timeout(Duration.ofMillis(cfg.getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            boolean success = body.path("success").asBoolean(false);
            if (!success) {
                log.warn("Turnstile rejected token: {}", body.path("error-codes"));
            }
            return success;
        } catch (Exception ex) {
            log.error("Turnstile siteverify call failed", ex);
            return false;
        }
    }

    private String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(UriUtils.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(UriUtils.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
