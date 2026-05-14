package com.auraia.backend.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AppProperties properties = new AppProperties();
    private final HtmlEmailRenderer renderer = mock(HtmlEmailRenderer.class);
    private final EmailDeliveryService emailDeliveryService = mock(EmailDeliveryService.class);
    private final VerificationEmailService service = new VerificationEmailService(
        mailSender,
        properties,
        renderer,
        emailDeliveryService
    );

    @Test
    void sendsHashRouterVerificationLinkForFrontend() throws Exception {
        properties.setFrontendBaseUrl("http://localhost:5173/");
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("AURA IA <no-reply@example.com>");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(renderer.render(anyString(), anyMap())).thenReturn("<html><body>Hola Maria, Verificar cuenta</body></html>");

        service.sendVerificationEmail(User.builder().email("maria@example.com").name("Maria No Alt").build(), "abc token");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(renderer).render(anyString(), varsCaptor.capture());
        assertThat(varsCaptor.getValue().get("actionUrl"))
            .isEqualTo("http://localhost:5173/#/verify-email?token=abc%20token");
        assertThat(varsCaptor.getValue().get("name")).isEqualTo("Maria");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();

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

        service.sendVerificationEmail(User.builder().email("maria@example.com").name("Maria").build(), "raw-token");

        verify(emailDeliveryService, never()).isSuppressed(anyString());
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
