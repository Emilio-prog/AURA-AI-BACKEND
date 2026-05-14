package com.auraia.backend.services.push;

import com.auraia.backend.models.enums.WebPushNotificationType;
import java.util.LinkedHashMap;
import java.util.Map;

public record WebPushPayload(
    WebPushNotificationType type,
    String title,
    String body,
    String url
) {
    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type.name());
        payload.put("title", title);
        payload.put("body", body);
        payload.put("url", url);
        return payload;
    }
}
