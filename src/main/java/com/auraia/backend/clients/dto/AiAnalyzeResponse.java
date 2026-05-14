package com.auraia.backend.clients.dto;

import java.util.List;

public record AiAnalyzeResponse(String sentiment, double score, List<String> emotions) {
}
