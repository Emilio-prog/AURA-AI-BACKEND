package com.auraia.backend.clients;

import com.auraia.backend.clients.dto.AiAnalyzeRequest;
import com.auraia.backend.clients.dto.AiAnalyzeResponse;
import com.auraia.backend.clients.dto.AiChatRequest;
import com.auraia.backend.clients.dto.AiChatResponse;
import com.auraia.backend.config.AppProperties;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class AiAnalyzerClient {

    private final AppProperties properties;
    private final RestClient restClient;

    public AiAnalyzerClient(AppProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getAi().getTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getAi().getTimeoutMs()));
        this.restClient = builder
            .baseUrl(properties.getAi().getServiceUrl())
            .requestFactory(requestFactory)
            .build();
    }

    public AiAnalyzeResponse analyzeSentiment(String text) {
        if (!properties.getAi().isEnabled()) {
            return mockAnalyze(text);
        }
        try {
            AiAnalyzeResponse response = restClient.post()
                .uri("/analyze")
                .body(new AiAnalyzeRequest(text))
                .retrieve()
                .body(AiAnalyzeResponse.class);
            return response == null ? mockAnalyze(text) : response;
        } catch (RestClientException ex) {
            log.warn("AI analyze service unavailable; returning mock response");
            return mockAnalyze(text);
        }
    }

    public AiChatResponse chat(List<Map<String, Object>> history, String message) {
        if (!properties.getAi().isEnabled()) {
            return mockChat(message);
        }
        try {
            AiChatResponse response = restClient.post()
                .uri("/chat")
                .body(new AiChatRequest(history, message))
                .retrieve()
                .body(AiChatResponse.class);
            return response == null ? mockChat(message) : response;
        } catch (RestClientException ex) {
            log.warn("AI chat service unavailable; returning mock response");
            return mockChat(message);
        }
    }

    private AiAnalyzeResponse mockAnalyze(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsCrisis(normalized)) {
            return new AiAnalyzeResponse("crisis", 0.95, List.of("fear", "distress"));
        }
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")) {
            return new AiAnalyzeResponse("negative", 0.82, List.of("anxiety"));
        }
        if (normalized.contains("bien") || normalized.contains("tranquil") || normalized.contains("mejor")) {
            return new AiAnalyzeResponse("positive", 0.76, List.of("calm"));
        }
        return new AiAnalyzeResponse("neutral", 0.55, List.of());
    }

    private AiChatResponse mockChat(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (containsCrisis(normalized)) {
            return new AiChatResponse(
                "Estoy contigo. Si hay riesgo inmediato para ti o para otra persona, llama ahora al 112. Si estas en Espana y necesitas apoyo urgente por ideacion suicida, contacta con el 024. Si puedes, avisa tambien a un contacto de confianza y alejate de cualquier objeto con el que puedas hacerte dano.",
                "crisis",
                "high",
                List.of("fear", "distress")
            );
        }
        if (normalized.contains("dormir") || normalized.contains("sueno") || normalized.contains("insomnio")) {
            return new AiChatResponse(
                "Dormir mal agota mucho. Probemos algo pequeno: baja la luz, deja el movil boca abajo y escribe una sola frase sobre lo que te preocupa. Luego hacemos tres ciclos de respiracion 4-4-6.",
                "supportive",
                "low",
                List.of("tiredness")
            );
        }
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")) {
            return new AiChatResponse(
                "Entiendo esa ansiedad. No tienes que resolver todo ahora. Dime una cosa concreta que notes en el cuerpo y la trabajamos paso a paso, sin prisa.",
                "supportive",
                "medium",
                List.of("anxiety")
            );
        }
        if (normalized.contains("bien") || normalized.contains("tranquil") || normalized.contains("mejor")) {
            return new AiChatResponse(
                "Me alegra leer eso. Guardemos esta referencia: que ha ayudado hoy, aunque sea pequeno? Nombrarlo puede servirte como recurso cuando el dia venga mas pesado.",
                "positive",
                "low",
                List.of("calm")
            );
        }
        return new AiChatResponse(
            "Gracias por contarmelo. Tiene sentido que te sientas asi. Podemos ordenar lo que pasa en tres partes: que ocurrio, que pensaste y que necesita tu cuerpo ahora mismo.",
            "supportive",
            "low",
            List.of()
        );
    }

    private boolean containsCrisis(String normalized) {
        return normalized.contains("panico")
            || normalized.contains("p\u00e1nico")
            || normalized.contains("crisis")
            || normalized.contains("sos")
            || normalized.contains("suicid")
            || normalized.contains("hacerme dano")
            || normalized.contains("hacerme da\u00f1o")
            || normalized.contains("autolesion");
    }
}
