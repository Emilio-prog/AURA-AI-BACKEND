package com.auraia.backend.services.sms;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.enums.NotificationStatus;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioSosSmsSender implements SosSmsSender {

    private final AppProperties properties;

    @PostConstruct
    void validateConfiguration() {
        AppProperties.SosSms config = properties.getSosSms();
        if (config.isEnabled()
            && (isBlank(config.getTwilioAccountSid())
                || isBlank(config.getTwilioAuthToken())
                || isBlank(config.getTwilioFromNumber()))) {
            throw new IllegalStateException(
                "TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER are required when SOS_SMS_ENABLED=true"
            );
        }
    }

    @Override
    public SosSmsResult send(String toPhoneNumber, String body) {
        AppProperties.SosSms config = properties.getSosSms();
        if (!config.isEnabled()) {
            return new SosSmsResult(NotificationStatus.MOCKED, null, "SOS SMS disabled; notification simulated");
        }
        try {
            Twilio.init(config.getTwilioAccountSid(), config.getTwilioAuthToken());
            Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(config.getTwilioFromNumber()),
                body
            ).create();
            return new SosSmsResult(NotificationStatus.SENT, message.getSid(), "Twilio SMS sent");
        } catch (RuntimeException ex) {
            log.warn("Twilio SOS SMS failed", ex);
            return new SosSmsResult(NotificationStatus.FAILED, null, blankToFallback(ex.getMessage()));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToFallback(String value) {
        return isBlank(value) ? "Twilio SMS failed" : value;
    }
}
