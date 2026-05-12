package com.auraia.backend.services.push;

public record WebPushSendResult(
    boolean success,
    int statusCode,
    String errorMessage
) {
    public static WebPushSendResult success(int statusCode) {
        return new WebPushSendResult(true, statusCode, null);
    }

    public static WebPushSendResult failure(int statusCode, String errorMessage) {
        return new WebPushSendResult(false, statusCode, errorMessage);
    }

    public boolean subscriptionRevoked() {
        return statusCode == 404 || statusCode == 410;
    }
}
