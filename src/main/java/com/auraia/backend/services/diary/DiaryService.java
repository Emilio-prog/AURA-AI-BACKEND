package com.auraia.backend.services.diary;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface DiaryService {

    PageResponse<DomainResponses.DiaryEntryResponse> list(Instant from, Instant to, Pageable pageable);

    DomainResponses.DiaryEntryResponse get(UUID id);

    DomainResponses.DiaryEntryResponse create(DomainRequests.DiaryEntryRequest request);

    DomainResponses.DiaryEntryResponse update(UUID id, DomainRequests.DiaryEntryRequest request);

    AuthResponses.MessageResponse delete(UUID id);
}
