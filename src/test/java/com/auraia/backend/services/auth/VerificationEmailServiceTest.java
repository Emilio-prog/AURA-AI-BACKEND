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
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class VerificationEmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AppProperties properties = new AppProperties();
    private final HtmlEmailRenderer renderer = mock(HtmlEmailRenderer.class);
    private final VerificationEmailService service = new VerificationEmailService(mailSender, properties, renderer);

    @Test
    void sendsHashRouterVerificationLinkForFrontend() {
        properties.setFrontendBaseUrl("http://localhost:5173/");
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("AURA IA <no-reply@example.com>");
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
        when(renderer.render(anyString(), anyMap())).thenReturn("<html>ok</html>");

        service.sendVerificationEmail(User.builder().email("maria@example.com").name("Maria").build(), "abc token");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(renderer).render(anyString(), varsCaptor.capture());
        assertThat(varsCaptor.getValue().get("actionUrl"))
            .isEqualTo("http://localhost:5173/#/verify-email?token=abc%20token");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void doesNotSendWhenEmailIsDisabled() {
        properties.getEmail().setEnabled(false);

        service.sendVerificationEmail(User.builder().email("maria@example.com").name("Maria").build(), "raw-token");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
