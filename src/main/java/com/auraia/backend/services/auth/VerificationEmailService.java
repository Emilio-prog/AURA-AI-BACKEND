package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public void sendVerificationEmail(User user, String rawToken) {
        String link = verificationLink(rawToken);
        if (!properties.getEmail().isEnabled()) {
            log.info("Email disabled. Verification link generated for {}", user.getEmail());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFrom());
        message.setTo(user.getEmail());
        message.setSubject("Verifica tu cuenta de AURA IA");
        message.setText("""
            Hola,

            Bienvenido a AURA IA. Verifica tu email abriendo este enlace:

            %s

            El enlace caduca automaticamente.
            """.formatted(link));
        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
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
}
