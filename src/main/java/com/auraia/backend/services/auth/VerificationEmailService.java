package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;
    private final HtmlEmailRenderer renderer;
    private final EmailDeliveryService emailDeliveryService;

    public void sendVerificationEmail(User user, String rawToken) {
        String link = verificationLink(rawToken);
        if (!properties.getEmail().isEnabled()) {
            log.info("Email disabled. Verification link generated for {}", user.getEmail());
            return;
        }
        if (emailDeliveryService.isSuppressed(user.getEmail())) {
            log.warn("Skipping verification email: recipient suppressed ({})", user.getEmail());
            return;
        }

        String subject = "Verifica tu cuenta de AURA IA";
        String html = renderer.render("email/verify", Map.of(
            "subject", subject,
            "title", "Verifica tu correo",
            "name", user.getName(),
            "actionUrl", link
        ));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MailException | MessagingException ex) {
            log.error("Verification email could not be sent to {}", user.getEmail(), ex);
            throw new IllegalStateException("Verification email failed", ex);
        }
    }

    private String verificationLink(String rawToken) {
        String frontendBaseUrl = properties.getFrontendBaseUrl().replaceAll("/+$", "");
        String encodedToken = UriUtils.encode(rawToken, StandardCharsets.UTF_8);
        return frontendBaseUrl + "/#/verify-email?token=" + encodedToken;
    }
}
