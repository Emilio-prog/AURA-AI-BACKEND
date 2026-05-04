package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.services.mood.MoodService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mood")
public class MoodController {

    private final MoodService moodService;

    @Operation(summary = "List mood logs")
    @GetMapping
    public PageResponse<DomainResponses.MoodLogResponse> list(@RequestParam(required = false) Instant from,
                                                              @RequestParam(required = false) Instant to,
                                                              @PageableDefault(size = 30) Pageable pageable) {
        return moodService.list(from, to, pageable);
    }

    @Operation(summary = "Create mood log")
    @PostMapping
    public DomainResponses.MoodLogResponse create(@Valid @RequestBody DomainRequests.MoodLogRequest request) {
        return moodService.create(request);
    }

    @Operation(summary = "Get aggregated mood stats")
    @GetMapping("/stats")
    public DomainResponses.MoodStatsResponse stats(@RequestParam(required = false) Instant from,
                                                   @RequestParam(required = false) Instant to) {
        return moodService.stats(from, to);
    }

    @Operation(summary = "Delete mood log")
    @DeleteMapping("/{id}")
    public AuthResponses.MessageResponse delete(@PathVariable UUID id) {
        return moodService.delete(id);
    }
}
