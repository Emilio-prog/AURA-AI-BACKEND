package com.auraia.backend.services.contact;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import java.util.List;
import java.util.UUID;

public interface ContactService {

    List<DomainResponses.ContactResponse> list();

    DomainResponses.ContactResponse create(DomainRequests.ContactRequest request);

    DomainResponses.ContactResponse update(UUID id, DomainRequests.ContactRequest request);

    AuthResponses.MessageResponse delete(UUID id);
}
