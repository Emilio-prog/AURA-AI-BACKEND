package com.auraia.backend.services.push;

import com.auraia.backend.models.enums.WebPushNotificationType;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebPushPayload {
    private WebPushNotificationType type;
    private String title;
    private String body;
    private String url;

    public WebPushPayload() {
    }

    public WebPushPayload(WebPushNotificationType type, String title, String body, String url) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.url = url;
    }

    public WebPushNotificationType getType() {
        return type;
    }

    public void setType(WebPushNotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type.name());
        payload.put("title", title);
        payload.put("body", body);
        payload.put("url", url);
        return payload;
    }
}
