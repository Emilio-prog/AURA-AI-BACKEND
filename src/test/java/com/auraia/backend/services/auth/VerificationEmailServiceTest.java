package com.auraia.backend.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class VerificationEmailServiceTest {

    private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    private final AppProperties properties = new AppProperties();
    private final VerificationEmailService service = new VerificationEmailService(mailSender, properties);

    @Test
    void sendsHashRouterVerificationLinkForFrontend() throws Exception {
        properties.setFrontendBaseUrl("http://localhost:5173/");
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("AURA IA <no-reply@example.com>");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendVerificationEmail(User.builder().email("maria@example.com").name("Maria No Alt").build(), "abc token");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();

        assertThat(message.getFrom()[0].toString()).isEqualTo("AURA IA <no-reply@example.com>");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("maria@example.com");
        assertThat(message.getSubject()).isEqualTo("Verifica tu cuenta de AURA IA");
        assertThat(extractContent(message.getContent()))
            .contains("http://localhost:5173/#/verify-email?token=abc%20token")
            .contains("Verificar cuenta")
            .contains("Maria")
            .doesNotContain("No Alt")
            .doesNotContain("alternativo");
    }

    @Test
    void doesNotSendWhenEmailIsDisabled() {
        properties.getEmail().setEnabled(false);

        service.sendVerificationEmail(User.builder().email("maria@example.com").build(), "raw-token");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private String extractContent(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                builder.append(extractContent(bodyPart.getContent()));
            }
            return builder.toString();
        }
        return "";
    }
}
