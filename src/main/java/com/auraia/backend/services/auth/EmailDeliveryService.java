package com.auraia.backend.services.auth;

import com.auraia.backend.models.entities.EmailEvent;
import com.auraia.backend.models.entities.EmailSuppression;
import com.auraia.backend.repositories.EmailEventRepository;
import com.auraia.backend.repositories.EmailSuppressionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryService {

    private final EmailEventRepository emailEventRepository;
    private final EmailSuppressionRepository emailSuppressionRepository;

    public boolean isSuppressed(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return emailSuppressionRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Transactional
    public void recordEvent(String eventType, String recipientEmail, String resendEmailId, String rawPayload) {
        EmailEvent event = EmailEvent.builder()
            .eventType(eventType)
            .recipientEmail(recipientEmail == null ? "" : recipientEmail.trim())
            .resendEmailId(resendEmailId)
            .payload(rawPayload)
            .receivedAt(Instant.now())
            .build();
        emailEventRepository.save(event);
    }

    @Transactional
    public void suppress(String email, String reason) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim();
        if (emailSuppressionRepository.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        EmailSuppression suppression = EmailSuppression.builder()
            .email(normalized)
            .reason(reason)
            .build();
        emailSuppressionRepository.save(suppression);
        log.warn("Email suppressed: {} ({})", normalized, reason);
    }
}
