package com.auraia.backend.services.sms;

import com.auraia.backend.models.enums.NotificationStatus;

public record SosSmsResult(
    NotificationStatus status,
    String providerMessageId,
    String details
) {
}
