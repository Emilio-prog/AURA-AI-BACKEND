package com.auraia.backend.clients.dto;

import java.util.List;
import java.util.Map;

public class AiChatRequest {
    private List<Map<String, Object>> history;
    private String message;

    public AiChatRequest() {
    }

    public AiChatRequest(List<Map<String, Object>> history, String message) {
        this.history = history;
        this.message = message;
    }

    public List<Map<String, Object>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, Object>> history) {
        this.history = history;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
