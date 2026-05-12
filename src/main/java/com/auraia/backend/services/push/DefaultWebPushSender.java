package com.auraia.backend.services.push;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.WebPushSubscription;
import com.auraia.backend.services.privacy.ContentCryptoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultWebPushSender implements WebPushSender {

    private final AppProperties properties;
    private final ContentCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    @PostConstruct
    void initialize() throws GeneralSecurityException {
        AppProperties.WebPush config = properties.getWebPush();
        if (!config.isEnabled()) {
            return;
        }
        if (isBlank(config.getVapidPublicKey()) || isBlank(config.getVapidPrivateKey())) {
            throw new IllegalStateException("WEB_PUSH_VAPID_PUBLIC_KEY and WEB_PUSH_VAPID_PRIVATE_KEY are required when WEB_PUSH_ENABLED=true");
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(config.getVapidPublicKey(), config.getVapidPrivateKey(), config.getSubject());
    }

    @Override
    public WebPushSendResult send(WebPushSubscription subscription, WebPushPayload payload) {
        if (!properties.getWebPush().isEnabled() || pushService == null) {
            return WebPushSendResult.failure(0, "web_push_disabled");
        }
        User user = subscription.getUser();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload.toMap());
            Notification notification = new Notification(
                cryptoService.decrypt(user.getId(), "push.endpoint", subscription.getEndpoint()),
                cryptoService.decrypt(user.getId(), "push.p256dh", subscription.getP256dh()),
                cryptoService.decrypt(user.getId(), "push.auth", subscription.getAuthSecret()),
                payloadJson
            );
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return WebPushSendResult.success(status);
            }
            return WebPushSendResult.failure(status, response.getStatusLine().getReasonPhrase());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize web push payload", ex);
        } catch (Exception ex) {
            log.warn("Web push send failed for subscription {}", subscription.getId(), ex);
            return WebPushSendResult.failure(0, ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
