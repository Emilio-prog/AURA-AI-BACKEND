package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public void sendVerificationEmail(User user, String rawToken) {
        String link = properties.getFrontendBaseUrl() + "/verify-email?token=" + rawToken;
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
        } catch (MailException ex) {
            log.error("Verification email could not be sent to {}", user.getEmail());
            throw ex;
        }
    }
}
