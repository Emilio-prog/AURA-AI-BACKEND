package com.auraia.backend.clients;

import com.auraia.backend.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GoogleGeminiContentGenerator implements GeminiContentGenerator {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

    private final AppProperties properties;
    private final RestClient restClient;

    public GoogleGeminiContentGenerator(AppProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(8_000);
        requestFactory.setReadTimeout(45_000);
        this.restClient = restClientBuilder
            .requestFactory(requestFactory)
            .baseUrl(GEMINI_BASE_URL)
            .build();
    }

    @Override
    public String generateText(String model, String prompt) {
        String apiKey = properties.getAi().getGeminiApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        Map<String, Object> request = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of(
                "temperature", 0.35,
                "maxOutputTokens", 900
            )
        );

        JsonNode response = restClient.post()
            .uri("/v1beta/models/{model}:generateContent", model)
            .header("X-goog-api-key", apiKey)
            .body(request)
            .retrieve()
            .body(JsonNode.class);

        JsonNode parts = response == null
            ? null
            : response.path("candidates").path(0).path("content").path("parts");
        if (parts == null || !parts.isArray()) {
            log.warn("Gemini REST response did not contain candidate text");
            throw new IllegalStateException("Gemini response missing text");
        }
        StringBuilder text = new StringBuilder();
        parts.forEach(part -> {
            String value = part.path("text").asText("");
            if (StringUtils.hasText(value)) {
                text.append(value);
            }
        });
        if (!StringUtils.hasText(text.toString())) {
            log.warn("Gemini REST response text was empty");
            throw new IllegalStateException("Gemini response text was empty");
        }
        return text.toString().trim();
    }
}
