package com.auraia.backend.services.mood;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface MoodService {

    PageResponse<DomainResponses.MoodLogResponse> list(Instant desde, Instant hasta, Pageable paginacion);

    DomainResponses.MoodLogResponse create(DomainRequests.MoodLogRequest peticion);

    DomainResponses.MoodStatsResponse stats(Instant desde, Instant hasta);

    AuthResponses.MessageResponse delete(UUID id);
}
