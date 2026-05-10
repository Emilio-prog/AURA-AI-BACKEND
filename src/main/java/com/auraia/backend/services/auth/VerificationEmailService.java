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
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
            "name", greetingName(user),
            "actionUrl", link,
            "ttlHours", properties.getEmail().getVerificationTokenTtlHours()
        ));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(plainText(link), html);
            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MessagingException ex) {
            log.error("Verification email could not be prepared for {}", user.getEmail());
            throw new MailPreparationException("Verification email could not be prepared", ex);
        } catch (MailException ex) {
            log.error("Verification email could not be sent to {}", user.getEmail());
            throw ex;
        }
    }

    private String verificationLink(String rawToken) {
        String frontendBaseUrl = properties.getFrontendBaseUrl().replaceAll("/+$", "");
        String encodedToken = UriUtils.encode(rawToken, StandardCharsets.UTF_8);
        return frontendBaseUrl + "/#/verify-email?token=" + encodedToken;
    }

    private String plainText(String link) {
        return """
            Hola,

            Bienvenido a AURA IA. Verifica tu email abriendo este enlace:

            %s

            Este enlace caduca en %d horas. Si no has creado esta cuenta, puedes ignorar este mensaje.
            """.formatted(link, properties.getEmail().getVerificationTokenTtlHours());
    }

    private String greetingName(User user) {
        if (!StringUtils.hasText(user.getName())) {
            return "AURA";
        }
        return user.getName().trim().split("\\s+")[0];
    }
}
