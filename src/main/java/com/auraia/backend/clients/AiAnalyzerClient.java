package com.auraia.backend.clients;

import com.auraia.backend.clients.dto.AiAnalyzeResponse;
import com.auraia.backend.clients.dto.AiChatResponse;
import com.auraia.backend.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AiAnalyzerClient {

    private static final String CRISIS_NOTICE =
        "Si hay riesgo inmediato para ti o para otra persona, llama ahora al 112. "
            + "Si estas en Espana y necesitas apoyo urgente por ideacion suicida, llama al 024.";
    private static final String SYSTEM_PROMPT = """
        Eres AURA IA, un asistente de apoyo emocional en espanol para una app de bienestar.
        No eres medico, psicologo ni terapeuta. No diagnostiques, no sustituyas atencion profesional,
        no recomiendes medicacion y no des instrucciones peligrosas.

        Responde con calidez, brevedad y pasos concretos de autocuidado emocional. Usa espanol claro.
        Solo menciona 112 o 024 si el mensaje habla de ideacion suicida, autolesion,
        dano a terceros o riesgo inmediato. Para ansiedad, panico, sueno o necesidad de calma
        sin autolesion, ofrece grounding y respiracion sin telefonos de emergencia.

        Devuelve solo el texto final para el usuario, sin JSON, sin etiquetas internas,
        sin markdown y sin listas largas.
        """;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiContentGenerator geminiContentGenerator;

    public AiAnalyzerClient(AppProperties properties,
                            ObjectMapper objectMapper,
                            GeminiContentGenerator geminiContentGenerator) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.geminiContentGenerator = geminiContentGenerator;
    }

    public AiAnalyzeResponse analyzeSentiment(String text) {
        if (!isGeminiReady()) {
            return mockAnalyze(text);
        }
        String normalized = normalize(text);
        if (containsEmergencyRisk(normalized)) {
            return mockAnalyze(text);
        }
        try {
            String response = geminiContentGenerator.generateText(
                properties.getAi().getGeminiModel(),
                "Clasifica el sentimiento de este texto sin diagnosticar. "
                    + "Responde solo con una palabra: positive, neutral, negative o crisis. Texto: " + safeText(text)
            );
            return analyzeFromText(text, response);
        } catch (RuntimeException ex) {
            log.warn("Gemini analyze unavailable; returning safe fallback");
            return mockAnalyze(text);
        }
    }

    public AiChatResponse chat(List<Map<String, Object>> history, String message) {
        String normalized = normalize(message);
        if (containsEmergencyRisk(normalized)) {
            return mockChat(message);
        }
        if (!isGeminiReady()) {
            return mockChat(message);
        }
        try {
            String reply = geminiContentGenerator.generateText(
                properties.getAi().getGeminiModel(),
                chatPrompt(limitHistory(history), message)
            );
            AiChatResponse response = new AiChatResponse(
                reply,
                sentimentFor(message, reply),
                riskLevelFor(message, reply),
                emotionsFor(message, reply)
            );
            return applySafetyGuard(response, message);
        } catch (RuntimeException ex) {
            log.warn("Gemini chat unavailable; returning safe fallback");
            return mockChat(message);
        }
    }

    private boolean isGeminiReady() {
        return properties.getAi().isEnabled() && StringUtils.hasText(properties.getAi().getGeminiApiKey());
    }

    private String chatPrompt(List<Map<String, Object>> history, String message) {
        try {
            return SYSTEM_PROMPT
                + "\nHistorial reciente de esta sesion en JSON:\n"
                + objectMapper.writeValueAsString(history)
                + "\nMensaje actual del usuario:\n"
                + safeText(message);
        } catch (JsonProcessingException ex) {
            return SYSTEM_PROMPT + "\nMensaje actual del usuario:\n" + safeText(message);
        }
    }

    private List<Map<String, Object>> limitHistory(List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int max = Math.max(1, properties.getAi().getMaxHistoryMessages());
        int from = Math.max(0, history.size() - max);
        return new ArrayList<>(history.subList(from, history.size()));
    }

    private AiChatResponse applySafetyGuard(AiChatResponse response, String message) {
        boolean highRisk = "high".equalsIgnoreCase(response.riskLevel())
            || containsEmergencyRisk(normalize(message))
            || containsEmergencyRisk(normalize(response.reply()));
        if (!highRisk) {
            return response;
        }
        String reply = response.reply();
        if (!reply.contains("112") || !reply.contains("024")) {
            reply = CRISIS_NOTICE + "\n\n" + reply;
        }
        return new AiChatResponse(reply, "crisis", "high", response.emotions());
    }

    private String normalizeRisk(String value) {
        String normalized = normalize(value);
        if ("high".equals(normalized) || "medium".equals(normalized) || "low".equals(normalized)) {
            return normalized;
        }
        return "low";
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private AiAnalyzeResponse analyzeFromText(String original, String generated) {
        if (containsEmergencyRisk(normalize(original)) || normalize(generated).contains("crisis")) {
            return new AiAnalyzeResponse("crisis", 0.95, List.of("fear", "distress"));
        }
        String sentiment = normalize(generated);
        if (sentiment.contains("positive")) {
            return new AiAnalyzeResponse("positive", 0.76, List.of("calm"));
        }
        if (sentiment.contains("negative")) {
            return new AiAnalyzeResponse("negative", 0.82, emotionsFor(original, generated));
        }
        return new AiAnalyzeResponse("neutral", 0.55, emotionsFor(original, generated));
    }

    private String sentimentFor(String message, String reply) {
        String normalized = normalize(message + " " + reply);
        if (containsEmergencyRisk(normalized)) {
            return "crisis";
        }
        if (normalized.contains("bien") || normalized.contains("tranquil") || normalized.contains("mejor")) {
            return "positive";
        }
        return "supportive";
    }

    private String riskLevelFor(String message, String reply) {
        String normalized = normalize(message + " " + reply);
        if (containsEmergencyRisk(normalized)) {
            return "high";
        }
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")
            || containsDistressSignal(normalized)) {
            return "medium";
        }
        return "low";
    }

    private List<String> emotionsFor(String message, String reply) {
        String normalized = normalize(message + " " + reply);
        List<String> emotions = new ArrayList<>();
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")) {
            emotions.add("anxiety");
        }
        if (normalized.contains("dormir") || normalized.contains("sueno") || normalized.contains("insomnio")) {
            emotions.add("tiredness");
        }
        if (containsEmergencyRisk(normalized)) {
            emotions.add("distress");
        }
        return List.copyOf(emotions);
    }

    private AiAnalyzeResponse mockAnalyze(String text) {
        String normalized = normalize(text);
        if (containsEmergencyRisk(normalized)) {
            return new AiAnalyzeResponse("crisis", 0.95, List.of("fear", "distress"));
        }
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")
            || containsDistressSignal(normalized)) {
            return new AiAnalyzeResponse("negative", 0.82, List.of("anxiety"));
        }
        if (normalized.contains("bien") || normalized.contains("tranquil") || normalized.contains("mejor")) {
            return new AiAnalyzeResponse("positive", 0.76, List.of("calm"));
        }
        return new AiAnalyzeResponse("neutral", 0.55, List.of());
    }

    private AiChatResponse mockChat(String message) {
        String normalized = normalize(message);
        if (containsEmergencyRisk(normalized)) {
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
        if (normalized.contains("ansiedad") || normalized.contains("nervios") || normalized.contains("agobio")
            || containsDistressSignal(normalized)) {
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

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsDistressSignal(String normalized) {
        return containsEmergencyRisk(normalized)
            || normalized.contains("panico")
            || normalized.contains("p\u00e1nico")
            || normalized.contains("crisis")
            || normalized.contains("sos");
    }

    private boolean containsEmergencyRisk(String normalized) {
        return normalized.contains("suicid")
            || normalized.contains("matarme")
            || normalized.contains("morirme")
            || normalized.contains("quitarme la vida")
            || normalized.contains("no quiero vivir")
            || normalized.contains("hacerme dano")
            || normalized.contains("hacerme da\u00f1o")
            || normalized.contains("autolesion")
            || normalized.contains("autolesi\u00f3n")
            || normalized.contains("hacer dano a alguien")
            || normalized.contains("hacer da\u00f1o a alguien")
            || normalized.contains("herir a alguien");
    }
}
