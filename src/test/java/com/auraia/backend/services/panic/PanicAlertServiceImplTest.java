package com.auraia.backend.services.panic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.PanicAlert;
import com.auraia.backend.models.entities.PanicNotificationResult;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.enums.NotificationStatus;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.PanicAlertRepository;
import com.auraia.backend.repositories.PanicNotificationResultRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.UserPrincipal;
import com.auraia.backend.services.privacy.TestContentCryptoService;
import com.auraia.backend.services.sms.SosSmsResult;
import com.auraia.backend.services.sms.SosSmsSender;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PanicAlertServiceImplTest {

    @Mock
    PanicAlertRepository panicAlertRepository;
    @Mock
    PanicNotificationResultRepository notificationResultRepository;
    @Mock
    ContactRepository contactRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SosSmsSender sosSmsSender;

    PanicAlertServiceImpl service;
    UUID userId;
    User user;
    Contact contact;

    @BeforeEach
    void setUp() {
        service = new PanicAlertServiceImpl(
            panicAlertRepository,
            notificationResultRepository,
            contactRepository,
            userRepository,
            new TestContentCryptoService(),
            sosSmsSender
        );
        userId = UUID.randomUUID();
        user = User.builder()
            .email("emilio@example.com")
            .passwordHash("hash")
            .name("Emilio")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(userId);
        contact = Contact.builder()
            .user(user)
            .name("Ana")
            .phone("+34 600 000 001")
            .relationship("Hermana")
            .priority(1)
            .available(true)
            .sosEnabled(true)
            .build();
        contact.setId(UUID.randomUUID());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(panicAlertRepository.save(any(PanicAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationResultRepository.save(any(PanicNotificationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationResultRepository.findByAlert(any(PanicAlert.class))).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void triggerWithContactIdSendsSmsToSelectedContact() {
        when(contactRepository.findByIdAndUser(contact.getId(), user)).thenReturn(Optional.of(contact));
        when(sosSmsSender.send("+34 600 000 001", "AURA IA: Emilio necesita apoyo ahora. Puedes llamarle o escribirle?"))
            .thenReturn(new SosSmsResult(NotificationStatus.SENT, "SM123", "sent"));

        service.trigger(new DomainRequests.PanicTriggerRequest(
            "necesito apoyo",
            contact.getId(),
            Map.of("source", "dashboard_sos")
        ));

        verify(sosSmsSender).send(eq("+34 600 000 001"), contains("Emilio necesita apoyo ahora"));
        ArgumentCaptor<PanicNotificationResult> captor = ArgumentCaptor.forClass(PanicNotificationResult.class);
        verify(notificationResultRepository).save(captor.capture());
        PanicNotificationResult result = captor.getValue();
        assertThat(result.getContact()).isSameAs(contact);
        assertThat(result.getChannel()).isEqualTo("SMS");
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getDetails()).contains("SM123");
    }

    @Test
    void triggerWithDisabledSosContactDoesNotSendSms() {
        contact.setSosEnabled(false);
        when(contactRepository.findByIdAndUser(contact.getId(), user)).thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> service.trigger(new DomainRequests.PanicTriggerRequest(
            null,
            contact.getId(),
            Map.of()
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.sos_contact_unavailable");

        verify(sosSmsSender, never()).send(any(), any());
    }
}
