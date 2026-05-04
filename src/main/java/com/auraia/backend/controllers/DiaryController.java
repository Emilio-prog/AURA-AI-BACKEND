package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.services.diary.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/diary")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "List diary entries")
    @GetMapping
    public PageResponse<DomainResponses.DiaryEntryResponse> list(@RequestParam(required = false) Instant from,
                                                                 @RequestParam(required = false) Instant to,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        return diaryService.list(from, to, pageable);
    }

    @Operation(summary = "Get diary entry")
    @GetMapping("/{id}")
    public DomainResponses.DiaryEntryResponse get(@PathVariable UUID id) {
        return diaryService.get(id);
    }

    @Operation(summary = "Create diary entry")
    @PostMapping
    public DomainResponses.DiaryEntryResponse create(@Valid @RequestBody DomainRequests.DiaryEntryRequest request) {
        return diaryService.create(request);
    }

    @Operation(summary = "Update diary entry")
    @PutMapping("/{id}")
    public DomainResponses.DiaryEntryResponse update(@PathVariable UUID id,
                                                     @Valid @RequestBody DomainRequests.DiaryEntryRequest request) {
        return diaryService.update(id, request);
    }

    @Operation(summary = "Delete diary entry")
    @DeleteMapping("/{id}")
    public AuthResponses.MessageResponse delete(@PathVariable UUID id) {
        return diaryService.delete(id);
    }
}
