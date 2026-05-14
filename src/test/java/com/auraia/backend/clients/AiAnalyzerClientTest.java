package com.auraia.backend.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.auraia.backend.clients.dto.AiChatResponse;
import com.auraia.backend.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiAnalyzerClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mockChatReturnsSpanishCrisisSafetyResponse() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(false);
        AiAnalyzerClient client = new AiAnalyzerClient(properties, objectMapper, (model, prompt) -> "{}");

        AiChatResponse response = client.chat(List.of(), "tengo una crisis y puedo hacerme dano");

        assertThat(response.riskLevel()).isEqualTo("high");
        assertThat(response.reply()).contains("112").contains("024");
    }

    @Test
    void mockChatIsDeterministicForAnxiety() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(false);
        AiAnalyzerClient client = new AiAnalyzerClient(properties, objectMapper, (model, prompt) -> "{}");

        AiChatResponse first = client.chat(List.of(), "siento ansiedad");
        AiChatResponse second = client.chat(List.of(), "siento ansiedad");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void enabledAiWithoutApiKeyFallsBackSafely() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(true);
        AiAnalyzerClient client = new AiAnalyzerClient(properties, objectMapper, (model, prompt) -> {
            throw new AssertionError("Gemini should not be called without an API key");
        });

        AiChatResponse response = client.chat(List.of(), "siento ansiedad");

        assertThat(response.reply()).contains("ansiedad");
        assertThat(response.riskLevel()).isEqualTo("medium");
    }

    @Test
    void validGeminiTextIsReturnedAsChatResponse() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(true);
        properties.getAi().setGeminiApiKey("test-key");
        AiAnalyzerClient client = new AiAnalyzerClient(
            properties,
            objectMapper,
            (model, prompt) -> "Estoy aqui contigo. Respira lento."
        );

        AiChatResponse response = client.chat(List.of(), "me siento inquieto");

        assertThat(response.reply()).contains("Respira lento");
        assertThat(response.sentiment()).isEqualTo("supportive");
        assertThat(response.riskLevel()).isEqualTo("low");
        assertThat(response.emotions()).isEmpty();
    }

    @Test
    void geminiExceptionFallsBackSafely() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(true);
        properties.getAi().setGeminiApiKey("test-key");
        AiAnalyzerClient client = new AiAnalyzerClient(
            properties,
            objectMapper,
            (model, prompt) -> {
                throw new IllegalStateException("Gemini unavailable");
            }
        );

        AiChatResponse response = client.chat(List.of(), "no puedo dormir");

        assertThat(response.reply()).contains("Dormir mal");
        assertThat(response.riskLevel()).isEqualTo("low");
    }

    @Test
    void highRiskGeminiResponseIsForcedToContainEmergencyNumbers() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(true);
        properties.getAi().setGeminiApiKey("test-key");
        AiAnalyzerClient client = new AiAnalyzerClient(
            properties,
            objectMapper,
            (model, prompt) -> "Estoy contigo ahora. Hablar de suicidio es una senal de pedir ayuda inmediata."
        );

        AiChatResponse response = client.chat(List.of(), "me siento al limite");

        assertThat(response.riskLevel()).isEqualTo("high");
        assertThat(response.sentiment()).isEqualTo("crisis");
        assertThat(response.reply()).contains("112").contains("024");
    }

    @Test
    void anxietyCrisisWordDoesNotForceEmergencyNumbers() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(true);
        properties.getAi().setGeminiApiKey("test-key");
        AiAnalyzerClient client = new AiAnalyzerClient(
            properties,
            objectMapper,
            (model, prompt) -> "Cuando aparece una crisis de ansiedad, prueba a notar los pies y respirar lento."
        );

        AiChatResponse response = client.chat(List.of(), "necesito calmarme");

        assertThat(response.riskLevel()).isEqualTo("medium");
        assertThat(response.sentiment()).isEqualTo("supportive");
        assertThat(response.reply()).doesNotContain("112").doesNotContain("024");
    }
}
