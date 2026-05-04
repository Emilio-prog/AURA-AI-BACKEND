package com.auraia.backend.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.auraia.backend.clients.dto.AiChatResponse;
import com.auraia.backend.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AiAnalyzerClientTest {

    @Test
    void mockChatReturnsSpanishCrisisSafetyResponse() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(false);
        AiAnalyzerClient client = new AiAnalyzerClient(properties, RestClient.builder());

        AiChatResponse response = client.chat(List.of(), "tengo una crisis y puedo hacerme dano");

        assertThat(response.riskLevel()).isEqualTo("high");
        assertThat(response.reply()).contains("112").contains("024");
    }

    @Test
    void mockChatIsDeterministicForAnxiety() {
        AppProperties properties = new AppProperties();
        properties.getAi().setEnabled(false);
        AiAnalyzerClient client = new AiAnalyzerClient(properties, RestClient.builder());

        AiChatResponse first = client.chat(List.of(), "siento ansiedad");
        AiChatResponse second = client.chat(List.of(), "siento ansiedad");

        assertThat(first).isEqualTo(second);
    }
}
