package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
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

    public void sendVerificationEmail(User user, String rawToken) {
        String link = verificationLink(rawToken);
        if (!properties.getEmail().isEnabled()) {
            log.info("Email disabled. Verification link generated for {}", user.getEmail());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject("Verifica tu cuenta de AURA IA");
            helper.setText(plainText(link), htmlText(user, link));
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

    private String htmlText(User user, String link) {
        String displayName = escapeHtml(greetingName(user));
        String safeLink = escapeHtml(link);
        long ttlHours = properties.getEmail().getVerificationTokenTtlHours();
        return """
            <!doctype html>
            <html lang="es">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Verifica tu cuenta de AURA IA</title>
              </head>
              <body style="margin:0;background:#f4f0ff;color:#111827;font-family:Arial,Helvetica,sans-serif;">
                <div style="display:none;max-height:0;overflow:hidden;color:transparent;">
                  Confirma tu email para activar tu panel personal de AURA IA.
                </div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f0ff;margin:0;padding:28px 12px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border:3px solid #111827;border-radius:18px;box-shadow:8px 8px 0 #111827;overflow:hidden;">
                        <tr>
                          <td style="background:#7c3aed;color:#ffffff;padding:24px 28px;border-bottom:3px solid #111827;">
                            <div style="font-size:14px;letter-spacing:0.12em;font-weight:800;text-transform:uppercase;">AURA IA</div>
                            <h1 style="margin:14px 0 0;font-size:30px;line-height:1.08;font-weight:900;">Verifica tu correo</h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:30px 28px 10px;">
                            <p style="margin:0 0 16px;font-size:18px;line-height:1.55;">Hola %s,</p>
                            <p style="margin:0 0 20px;font-size:16px;line-height:1.7;">Tu cuenta de AURA IA ya esta preparada. Confirma este email para proteger el acceso a tu diario, mood tracker y panel personal.</p>
                            <p style="margin:28px 0;">
                              <a href="%s" style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;font-size:16px;font-weight:800;border:3px solid #111827;border-radius:12px;padding:14px 22px;box-shadow:5px 5px 0 #7c3aed;">Verificar cuenta</a>
                            </p>
                            <p style="margin:0 0 18px;font-size:14px;line-height:1.65;color:#4b5563;">El enlace caduca en %d horas. Si no has creado esta cuenta, puedes ignorar este mensaje.</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#111827;color:#ffffff;padding:18px 28px;font-size:12px;line-height:1.6;">
                            AURA IA protege tu cuenta con verificacion de email y acceso JWT. Nunca compartas este enlace.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(displayName, safeLink, ttlHours);
    }

    private String greetingName(User user) {
        if (!StringUtils.hasText(user.getName())) {
            return "AURA";
        }
        return user.getName().trim().split("\\s+")[0];
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
