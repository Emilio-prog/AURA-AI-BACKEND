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
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;
    private final HtmlEmailRenderer renderer;
    private final EmailDeliveryService emailDeliveryService;

    public void sendResetEmail(User user, String rawToken) {
        String link = resetLink(rawToken);
        if (!properties.getEmail().isEnabled()) {
            log.info("Email disabled. Reset link generated for {}", user.getEmail());
            return;
        }
        if (emailDeliveryService.isSuppressed(user.getEmail())) {
            log.warn("Skipping password reset email: recipient suppressed ({})", user.getEmail());
            return;
        }

        String subject = "Restablece tu contraseña de AURA IA";
        String html = renderer.render("email/reset-password", Map.of(
            "subject", subject,
            "title", "Restablece tu contraseña",
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
            log.info("Password reset email sent to {}", user.getEmail());
        } catch (MailException | MessagingException ex) {
            log.error("Password reset email could not be sent to {}", user.getEmail(), ex);
            throw new IllegalStateException("Password reset email failed", ex);
        }
    }

    private String resetLink(String rawToken) {
        String frontendBaseUrl = properties.getFrontendBaseUrl().replaceAll("/+$", "");
        String encodedToken = UriUtils.encode(rawToken, StandardCharsets.UTF_8);
        return frontendBaseUrl + "/#/reset-password?token=" + encodedToken;
    }
}
