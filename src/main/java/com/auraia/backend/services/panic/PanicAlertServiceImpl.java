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
import com.auraia.backend.services.privacy.ContentCryptoService;
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
    private final ContentCryptoService contentCryptoService;

    @Override
    @Transactional
    public DomainResponses.PanicAlertResponse trigger(DomainRequests.PanicTriggerRequest request) {
        User user = currentUser();
        String notes = blankToNull(request.notes());
        Map<String, Object> contextJson = request.contextJson() == null ? new LinkedHashMap<>() : request.contextJson();
        PanicAlert alert = PanicAlert.builder()
            .user(user)
            .triggeredAt(Instant.now())
            .notes(contentCryptoService.encrypt(user.getId(), "panic.notes", notes))
            .contextJson(contentCryptoService.encryptJsonMap(user.getId(), "panic.context", contextJson))
            .build();
        PanicAlert saved = panicAlertRepository.save(alert);
        List<Contact> contacts = contactRepository.findByUserAndSosEnabledTrueOrderByPriorityAsc(user);
        for (Contact contact : contacts) {
            String contactName = contentCryptoService.decrypt(user.getId(), "contact.name", contact.getName());
            notificationResultRepository.save(PanicNotificationResult.builder()
                .alert(saved)
                .contact(contact)
                .channel("MOCK")
                .status(NotificationStatus.MOCKED)
                .details(contentCryptoService.encrypt(user.getId(), "panic.notification-details", "Mock notification queued for " + contactName))
                .build());
        }
        return toResponse(saved, user);
    }

    @Override
    @Transactional
    public PageResponse<DomainResponses.PanicAlertResponse> history(Pageable pageable) {
        User user = currentUser();
        return PageResponse.from(panicAlertRepository.findByUser(user, pageable).map(alert -> toResponse(alert, user)));
    }

    @Override
    @Transactional
    public DomainResponses.PanicAlertResponse resolve(UUID id, DomainRequests.PanicResolveRequest request) {
        User user = currentUser();
        PanicAlert alert = panicAlertRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Panic alert not found"));
        alert.setResolvedAt(Instant.now());
        if (request.notes() != null && !request.notes().isBlank()) {
            alert.setNotes(contentCryptoService.encrypt(user.getId(), "panic.notes", request.notes().trim()));
        }
        return toResponse(panicAlertRepository.save(alert), user);
    }

    private DomainResponses.PanicAlertResponse toResponse(PanicAlert alert, User user) {
        String notes = contentCryptoService.decrypt(user.getId(), "panic.notes", alert.getNotes());
        Map<String, Object> contextJson = alert.getContextJson() == null
            ? new LinkedHashMap<>()
            : contentCryptoService.decryptJsonMap(user.getId(), "panic.context", alert.getContextJson());
        if (contentCryptoService.isEnabled()) {
            boolean notesNeedProtection = alert.getNotes() != null && !contentCryptoService.isEncrypted(alert.getNotes());
            boolean contextNeedsProtection = hasLegacyJsonStrings(alert.getContextJson());
            if (notesNeedProtection || contextNeedsProtection) {
                if (notesNeedProtection) {
                    alert.setNotes(contentCryptoService.encrypt(user.getId(), "panic.notes", notes));
                }
                if (contextNeedsProtection) {
                    alert.setContextJson(contentCryptoService.encryptJsonMap(user.getId(), "panic.context", contextJson));
                }
                panicAlertRepository.save(alert);
            }
        }
        return new DomainResponses.PanicAlertResponse(
            alert.getId(),
            alert.getTriggeredAt(),
            alert.getResolvedAt(),
            notes,
            contextJson,
            notificationResultRepository.findByAlert(alert).stream().map(result -> toNotificationResponse(result, user)).toList(),
            alert.getCreatedAt(),
            alert.getUpdatedAt()
        );
    }

    private DomainResponses.PanicNotificationResponse toNotificationResponse(PanicNotificationResult result, User user) {
        String details = contentCryptoService.decrypt(user.getId(), "panic.notification-details", result.getDetails());
        if (contentCryptoService.isEnabled()) {
            boolean detailsNeedProtection = result.getDetails() != null && !contentCryptoService.isEncrypted(result.getDetails());
            if (detailsNeedProtection) {
                result.setDetails(contentCryptoService.encrypt(user.getId(), "panic.notification-details", details));
                notificationResultRepository.save(result);
            }
        }
        return new DomainResponses.PanicNotificationResponse(
            result.getId(),
            result.getContact() == null ? null : result.getContact().getId(),
            result.getContact() == null ? null : contentCryptoService.decrypt(user.getId(), "contact.name", result.getContact().getName()),
            result.getChannel(),
            result.getStatus(),
            details,
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

    private boolean hasLegacyJsonStrings(Object value) {
        if (value instanceof String text) {
            return !contentCryptoService.isEncrypted(text);
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(this::hasLegacyJsonStrings);
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(this::hasLegacyJsonStrings);
        }
        return false;
    }
}
