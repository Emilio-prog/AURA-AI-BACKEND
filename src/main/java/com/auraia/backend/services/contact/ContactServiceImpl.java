package com.auraia.backend.services.contact;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.mappers.ContactMapper;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.ContactRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
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
    private final ContactMapper contactMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DomainResponses.ContactResponse> list() {
        return contactRepository.findByUserOrderByPriorityAscNameAsc(currentUser()).stream()
            .map(contactMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public DomainResponses.ContactResponse create(DomainRequests.ContactRequest request) {
        Contact contact = Contact.builder()
            .user(currentUser())
            .name(request.name().trim())
            .phone(request.phone().trim())
            .relationship(blankToNull(request.relationship()))
            .priority(request.priority() == null ? 1 : request.priority())
            .available(request.available() == null || request.available())
            .sosEnabled(Boolean.TRUE.equals(request.sosEnabled()))
            .build();
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public DomainResponses.ContactResponse update(UUID id, DomainRequests.ContactRequest request) {
        Contact contact = findOwned(id);
        contact.setName(request.name().trim());
        contact.setPhone(request.phone().trim());
        contact.setRelationship(blankToNull(request.relationship()));
        contact.setPriority(request.priority() == null ? 1 : request.priority());
        contact.setAvailable(request.available() == null || request.available());
        contact.setSosEnabled(Boolean.TRUE.equals(request.sosEnabled()));
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        contactRepository.delete(findOwned(id));
        return new AuthResponses.MessageResponse("OK");
    }

    private Contact findOwned(UUID id) {
        return contactRepository.findByIdAndUser(id, currentUser())
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
