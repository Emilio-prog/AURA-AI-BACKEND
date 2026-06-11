package com.auraia.backend.clients.dto;

import java.util.List;
import java.util.Objects;

/**
 * Respuesta que devuelve despues de una conversacion.
 * Incluye el texto de respuesta y datos sencillos sobre el estado emocional.
 */
public class AiChatResponse {
    private String reply;
    private String sentiment;
    private String riskLevel;
    private List<String> emotions;

    public AiChatResponse() {
    }

    /**
     * Crea una respuesta completa del chat.
     */
    public AiChatResponse(String reply, String sentiment, String riskLevel, List<String> emotions) {
        this.reply = reply;
        this.sentiment = sentiment;
        this.riskLevel = riskLevel;
        this.emotions = emotions;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getEmotions() {
        return emotions;
    }

    public void setEmotions(List<String> emotions) {
        this.emotions = emotions;
    }

    /**
     * Compara dos respuestas para saber si tienen los mismos datos.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AiChatResponse response)) {
            return false;
        }
        return Objects.equals(reply, response.reply)
            && Objects.equals(sentiment, response.sentiment)
            && Objects.equals(riskLevel, response.riskLevel)
            && Objects.equals(emotions, response.emotions);
    }

    /**
     * Calcula un valor usando los campos de la respuesta.
     */
    @Override
    public int hashCode() {
        return Objects.hash(reply, sentiment, riskLevel, emotions);
    }
}
