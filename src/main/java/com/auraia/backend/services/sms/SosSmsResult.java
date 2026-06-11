package com.auraia.backend.services.sms;

import com.auraia.backend.models.enums.NotificationStatus;

/**
 * Resultado de enviar un SMS de emergencia.
 */
public class SosSmsResult {
    private NotificationStatus status;
    private String providerMessageId;
    private String details;

    public SosSmsResult() {
    }

    /**
     * Crea el resultado con estado, id del proveedor y detalles.
     */
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
