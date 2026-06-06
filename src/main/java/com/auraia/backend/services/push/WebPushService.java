package com.auraia.backend.services.push;

import com.auraia.backend.models.dto.request.PushRequests;
import com.auraia.backend.models.dto.response.PushResponses;
import com.auraia.backend.models.entities.MoodLog;
import com.auraia.backend.models.entities.User;
import java.time.Instant;

public interface WebPushService {

    PushResponses.PushConfigResponse config();

    PushResponses.PushSubscriptionResponse subscribe(PushRequests.SubscriptionRequest request);

    PushResponses.PushSubscriptionResponse disable(PushRequests.DisableSubscriptionRequest request);

    PushResponses.PushTestResponse test();

    void runScheduledReminders(Instant now);

    void enviarAlertaCaidaAnimo(User usuario, MoodLog registro, double mediaAnterior);
}
