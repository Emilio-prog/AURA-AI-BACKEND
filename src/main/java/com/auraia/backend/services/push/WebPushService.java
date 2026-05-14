package com.auraia.backend.services.push;

import com.auraia.backend.models.dto.request.PushRequests;
import com.auraia.backend.models.dto.response.PushResponses;
import java.time.Instant;

public interface WebPushService {

    PushResponses.PushConfigResponse config();

    PushResponses.PushSubscriptionResponse subscribe(PushRequests.SubscriptionRequest request);

    PushResponses.PushSubscriptionResponse disable(PushRequests.DisableSubscriptionRequest request);

    PushResponses.PushTestResponse test();

    void runScheduledReminders(Instant now);
}
