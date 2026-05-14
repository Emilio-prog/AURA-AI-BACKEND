package com.auraia.backend.clients.dto;

import java.util.List;

public record AiChatResponse(String reply, String sentiment, String riskLevel, List<String> emotions) {
}
