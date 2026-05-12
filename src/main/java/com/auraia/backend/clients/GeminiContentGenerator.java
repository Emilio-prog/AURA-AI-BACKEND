package com.auraia.backend.clients;

import java.util.Map;

public interface GeminiContentGenerator {

    String generateText(String model, String prompt);
}
