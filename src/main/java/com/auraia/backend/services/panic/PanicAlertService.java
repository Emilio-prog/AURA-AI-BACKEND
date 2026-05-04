package com.auraia.backend.services.panic;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PanicAlertService {

    DomainResponses.PanicAlertResponse trigger(DomainRequests.PanicTriggerRequest request);

    PageResponse<DomainResponses.PanicAlertResponse> history(Pageable pageable);

    DomainResponses.PanicAlertResponse resolve(UUID id, DomainRequests.PanicResolveRequest request);
}
