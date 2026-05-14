package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;
    private final HtmlEmailRenderer renderer;
    private final EmailDeliveryService emailDeliveryService;

    public void sendWelcomeEmail(User user) {
        try {
            if (!properties.getEmail().isEnabled()) {
                log.info("Email disabled. Skipping welcome email for {}", user.getEmail());
                return;
            }
            if (emailDeliveryService.isSuppressed(user.getEmail())) {
                log.warn("Skipping welcome email: recipient suppressed ({})", user.getEmail());
                return;
            }

            String dashboardUrl = properties.getFrontendBaseUrl().replaceAll("/+$", "") + "/#/dashboard";
            String subject = "Bienvenido/a a AURA IA";
            String html = renderer.render("email/welcome", Map.of(
                "subject", subject,
                "title", "Bienvenido/a a AURA IA",
                "name", user.getName(),
                "actionUrl", dashboardUrl
            ));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", user.getEmail());
        } catch (Exception ex) {
            log.error("Welcome email could not be sent to {}", user.getEmail(), ex);
        }
    }
}
