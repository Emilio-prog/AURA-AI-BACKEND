package com.auraia.backend.services.push;

public class WebPushSendResult {
    private boolean success;
    private int statusCode;
    private String errorMessage;

    public WebPushSendResult() {
    }

    public WebPushSendResult(boolean success, int statusCode, String errorMessage) {
        this.success = success;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    public static WebPushSendResult success(int statusCode) {
        return new WebPushSendResult(true, statusCode, null);
    }

    public static WebPushSendResult failure(int statusCode, String errorMessage) {
        return new WebPushSendResult(false, statusCode, errorMessage);
    }

    public boolean subscriptionRevoked() {
        return statusCode == 404 || statusCode == 410;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
