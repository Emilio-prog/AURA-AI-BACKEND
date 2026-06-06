package com.auraia.backend.services.sms;

import com.auraia.backend.models.enums.NotificationStatus;

public class SosSmsResult {
    private NotificationStatus status;
    private String providerMessageId;
    private String details;

    public SosSmsResult() {
    }

    public SosSmsResult(NotificationStatus status, String providerMessageId, String details) {
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.details = details;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
