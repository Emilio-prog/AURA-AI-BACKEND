package com.auraia.backend.services.contact;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.privacy.ContentCryptoService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContentCryptoService contentCryptoService;

    @Override
    @Transactional
    public List<DomainResponses.ContactResponse> list() {
        User user = currentUser();
        return contactRepository.findByUserOrderByPriorityAscNameAsc(user).stream()
            .map(contact -> decryptAndProtect(contact, user))
            .sorted(Comparator.comparingInt(DomainResponses.ContactResponse::priority)
                .thenComparing(response -> response.name().toLowerCase(java.util.Locale.ROOT)))
            .toList();
    }

    @Override
    @Transactional
    public DomainResponses.ContactResponse create(DomainRequests.ContactRequest request) {
        User user = currentUser();
        String name = request.name().trim();
        String phone = request.phone().trim();
        String relationship = blankToNull(request.relationship());
        Contact contact = Contact.builder()
            .user(user)
            .name(contentCryptoService.encrypt(user.getId(), "contact.name", name))
            .phone(contentCryptoService.encrypt(user.getId(), "contact.phone", phone))
            .relationship(contentCryptoService.encrypt(user.getId(), "contact.relationship", relationship))
            .priority(request.priority() == null ? 1 : request.priority())
            .available(request.available() == null || request.available())
            .sosEnabled(Boolean.TRUE.equals(request.sosEnabled()))
            .build();
        return response(contactRepository.save(contact), name, phone, relationship);
    }

    @Override
    @Transactional
    public DomainResponses.ContactResponse update(UUID id, DomainRequests.ContactRequest request) {
        User user = currentUser();
        String name = request.name().trim();
        String phone = request.phone().trim();
        String relationship = blankToNull(request.relationship());
        Contact contact = findOwned(id, user);
        contact.setName(contentCryptoService.encrypt(user.getId(), "contact.name", name));
        contact.setPhone(contentCryptoService.encrypt(user.getId(), "contact.phone", phone));
        contact.setRelationship(contentCryptoService.encrypt(user.getId(), "contact.relationship", relationship));
        contact.setPriority(request.priority() == null ? 1 : request.priority());
        contact.setAvailable(request.available() == null || request.available());
        contact.setSosEnabled(Boolean.TRUE.equals(request.sosEnabled()));
        return response(contactRepository.save(contact), name, phone, relationship);
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        contactRepository.delete(findOwned(id, currentUser()));
        return new AuthResponses.MessageResponse("OK");
    }

    private Contact findOwned(UUID id) {
        return findOwned(id, currentUser());
    }

    private Contact findOwned(UUID id, User user) {
        return contactRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DomainResponses.ContactResponse decryptAndProtect(Contact contact, User user) {
        String name = contentCryptoService.decrypt(user.getId(), "contact.name", contact.getName());
        String phone = contentCryptoService.decrypt(user.getId(), "contact.phone", contact.getPhone());
        String relationship = contentCryptoService.decrypt(user.getId(), "contact.relationship", contact.getRelationship());
        if (contentCryptoService.isEnabled()) {
            String encryptedName = encryptLegacyValue(user.getId(), "contact.name", contact.getName(), name);
            String encryptedPhone = encryptLegacyValue(user.getId(), "contact.phone", contact.getPhone(), phone);
            String encryptedRelationship = encryptLegacyValue(user.getId(), "contact.relationship", contact.getRelationship(), relationship);
            boolean changed = false;
            if (!java.util.Objects.equals(contact.getName(), encryptedName)) {
                contact.setName(encryptedName);
                changed = true;
            }
            if (!java.util.Objects.equals(contact.getPhone(), encryptedPhone)) {
                contact.setPhone(encryptedPhone);
                changed = true;
            }
            if (!java.util.Objects.equals(contact.getRelationship(), encryptedRelationship)) {
                contact.setRelationship(encryptedRelationship);
                changed = true;
            }
            if (changed) {
                contactRepository.save(contact);
            }
        }
        return response(contact, name, phone, relationship);
    }

    private DomainResponses.ContactResponse response(Contact contact, String name, String phone, String relationship) {
        return new DomainResponses.ContactResponse(
            contact.getId(),
            name,
            phone,
            relationship,
            contact.getPriority(),
            contact.isAvailable(),
            contact.isSosEnabled(),
            contact.getCreatedAt(),
            contact.getUpdatedAt()
        );
    }

    private String encryptLegacyValue(UUID userId, String scope, String storedValue, String plainText) {
        return storedValue != null && !contentCryptoService.isEncrypted(storedValue)
            ? contentCryptoService.encrypt(userId, scope, plainText)
            : storedValue;
    }
}
