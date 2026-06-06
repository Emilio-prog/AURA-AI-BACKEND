package com.auraia.backend.clients.dto;

import java.util.List;

public class AiAnalyzeResponse {
    private String sentiment;
    private double score;
    private List<String> emotions;

    public AiAnalyzeResponse() {
    }

    public AiAnalyzeResponse(String sentiment, double score, List<String> emotions) {
        this.sentiment = sentiment;
        this.score = score;
        this.emotions = emotions;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<String> getEmotions() {
        return emotions;
    }

    public void setEmotions(List<String> emotions) {
        this.emotions = emotions;
    }
}
