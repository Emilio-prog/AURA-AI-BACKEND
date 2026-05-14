package com.auraia.backend.controllers;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.services.auth.EmailDeliveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
public class ResendWebhookController {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String SECRET_PREFIX = "whsec_";

    private final AppProperties appProperties;
    private final EmailDeliveryService emailDeliveryService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Resend webhook receiver (bounces, complaints, deliveries)")
    @PostMapping("/resend")
    public ResponseEntity<Void> handleResend(
            @RequestHeader(value = "svix-id", required = false) String svixId,
            @RequestHeader(value = "svix-timestamp", required = false) String svixTimestamp,
            @RequestHeader(value = "svix-signature", required = false) String svixSignature,
            @RequestBody String body) {

        String secret = appProperties.getWebhook().getResendSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Resend webhook secret not configured. Rejecting request.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (!verifySignature(svixId, svixTimestamp, svixSignature, body, secret)) {
            log.warn("Resend webhook signature invalid (svixId={})", svixId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String type = root.path("type").asText("unknown");
            JsonNode data = root.path("data");
            String emailId = textOrNull(data.path("email_id"));
            String recipient = extractFirstRecipient(data);

            emailDeliveryService.recordEvent(type, recipient, emailId, body);

            switch (type) {
                case "email.bounced" -> emailDeliveryService.suppress(recipient, "BOUNCED");
                case "email.complained" -> emailDeliveryService.suppress(recipient, "COMPLAINED");
                default -> { /* sent, delivered, delayed, opened — solo registro */ }
            }

            log.info("Resend webhook processed: type={} recipient={} resendId={}", type, recipient, emailId);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Failed to process Resend webhook payload", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean verifySignature(String svixId, String svixTimestamp, String svixSignature,
                                     String body, String secret) {
        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            return false;
        }
        try {
            String key = secret.startsWith(SECRET_PREFIX) ? secret.substring(SECRET_PREFIX.length()) : secret;
            byte[] secretBytes = Base64.getDecoder().decode(key);
            String toSign = svixId + "." + svixTimestamp + "." + body;

            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            byte[] hmac = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(hmac);

            // Header may contain multiple signatures separated by spaces, e.g. "v1,abc v1,def".
            for (String sig : svixSignature.split(" ")) {
                String trimmed = sig.trim();
                int sep = trimmed.indexOf(',');
                if (sep <= 0) continue;
                String version = trimmed.substring(0, sep);
                String received = trimmed.substring(sep + 1);
                if ("v1".equals(version) && constantTimeEquals(expected, received)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.error("Resend webhook signature verification error", ex);
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
    }

    private String extractFirstRecipient(JsonNode data) {
        JsonNode to = data.path("to");
        if (to.isArray() && to.size() > 0) {
            return to.get(0).asText("");
        }
        if (to.isTextual()) {
            return to.asText("");
        }
        return "";
    }

    private String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText(null);
    }
}
