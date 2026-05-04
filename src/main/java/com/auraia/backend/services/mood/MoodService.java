package com.auraia.backend.services.mood;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface MoodService {

    PageResponse<DomainResponses.MoodLogResponse> list(Instant from, Instant to, Pageable pageable);

    DomainResponses.MoodLogResponse create(DomainRequests.MoodLogRequest request);

    DomainResponses.MoodStatsResponse stats(Instant from, Instant to);

    AuthResponses.MessageResponse delete(UUID id);
}
