package com.auraia.backend.services.panic;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.PanicAlert;
import com.auraia.backend.models.entities.PanicNotificationResult;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.enums.NotificationStatus;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.PanicAlertRepository;
import com.auraia.backend.repositories.PanicNotificationResultRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PanicAlertServiceImpl implements PanicAlertService {

    private final PanicAlertRepository panicAlertRepository;
    private final PanicNotificationResultRepository notificationResultRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DomainResponses.PanicAlertResponse trigger(DomainRequests.PanicTriggerRequest request) {
        User user = currentUser();
        PanicAlert alert = PanicAlert.builder()
            .user(user)
            .triggeredAt(Instant.now())
            .notes(blankToNull(request.notes()))
            .contextJson(request.contextJson() == null ? new LinkedHashMap<>() : request.contextJson())
            .build();
        PanicAlert saved = panicAlertRepository.save(alert);
        List<Contact> contacts = contactRepository.findByUserAndSosEnabledTrueOrderByPriorityAsc(user);
        for (Contact contact : contacts) {
            notificationResultRepository.save(PanicNotificationResult.builder()
                .alert(saved)
                .contact(contact)
                .channel("MOCK")
                .status(NotificationStatus.MOCKED)
                .details("Mock notification queued for " + contact.getName())
                .build());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DomainResponses.PanicAlertResponse> history(Pageable pageable) {
        return PageResponse.from(panicAlertRepository.findByUser(currentUser(), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public DomainResponses.PanicAlertResponse resolve(UUID id, DomainRequests.PanicResolveRequest request) {
        PanicAlert alert = panicAlertRepository.findByIdAndUser(id, currentUser())
            .orElseThrow(() -> new ResourceNotFoundException("Panic alert not found"));
        alert.setResolvedAt(Instant.now());
        if (request.notes() != null && !request.notes().isBlank()) {
            alert.setNotes(request.notes().trim());
        }
        return toResponse(panicAlertRepository.save(alert));
    }

    private DomainResponses.PanicAlertResponse toResponse(PanicAlert alert) {
        return new DomainResponses.PanicAlertResponse(
            alert.getId(),
            alert.getTriggeredAt(),
            alert.getResolvedAt(),
            alert.getNotes(),
            alert.getContextJson(),
            notificationResultRepository.findByAlert(alert).stream().map(this::toNotificationResponse).toList(),
            alert.getCreatedAt(),
            alert.getUpdatedAt()
        );
    }

    private DomainResponses.PanicNotificationResponse toNotificationResponse(PanicNotificationResult result) {
        return new DomainResponses.PanicNotificationResponse(
            result.getId(),
            result.getContact() == null ? null : result.getContact().getId(),
            result.getContact() == null ? null : result.getContact().getName(),
            result.getChannel(),
            result.getStatus(),
            result.getDetails(),
            result.getCreatedAt()
        );
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
