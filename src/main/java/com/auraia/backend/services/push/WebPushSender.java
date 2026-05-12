package com.auraia.backend.services.push;

import com.auraia.backend.models.entities.WebPushSubscription;

public interface WebPushSender {

    WebPushSendResult send(WebPushSubscription subscription, WebPushPayload payload);
}
