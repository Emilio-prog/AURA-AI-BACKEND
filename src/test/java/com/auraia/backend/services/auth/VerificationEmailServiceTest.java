package com.auraia.backend.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.models.entities.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class VerificationEmailServiceTest {

    private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    private final AppProperties properties = new AppProperties();
    private final VerificationEmailService service = new VerificationEmailService(mailSender, properties);

    @Test
    void sendsHashRouterVerificationLinkForFrontend() {
        properties.setFrontendBaseUrl("http://localhost:5173/");
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("AURA IA <no-reply@example.com>");

        service.sendVerificationEmail(User.builder().email("maria@example.com").build(), "abc token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("AURA IA <no-reply@example.com>");
        assertThat(message.getTo()).containsExactly("maria@example.com");
        assertThat(message.getText()).contains("http://localhost:5173/#/verify-email?token=abc%20token");
    }

    @Test
    void doesNotSendWhenEmailIsDisabled() {
        properties.getEmail().setEnabled(false);

        service.sendVerificationEmail(User.builder().email("maria@example.com").build(), "raw-token");

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
