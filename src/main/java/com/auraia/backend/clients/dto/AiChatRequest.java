package com.auraia.backend.clients.dto;

import java.util.List;
import java.util.Map;

public record AiChatRequest(List<Map<String, Object>> history, String message) {
}
