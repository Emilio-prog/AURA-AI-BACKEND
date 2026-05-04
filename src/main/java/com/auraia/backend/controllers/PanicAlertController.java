package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.services.panic.PanicAlertService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/panic")
public class PanicAlertController {

    private final PanicAlertService panicAlertService;

    @Operation(summary = "Trigger panic alert")
    @PostMapping("/trigger")
    public DomainResponses.PanicAlertResponse trigger(@Valid @RequestBody DomainRequests.PanicTriggerRequest request) {
        return panicAlertService.trigger(request);
    }

    @Operation(summary = "List panic alert history")
    @GetMapping("/history")
    public PageResponse<DomainResponses.PanicAlertResponse> history(@PageableDefault(size = 20) Pageable pageable) {
        return panicAlertService.history(pageable);
    }

    @Operation(summary = "Resolve panic alert")
    @PutMapping("/{id}/resolve")
    public DomainResponses.PanicAlertResponse resolve(@PathVariable UUID id,
                                                      @Valid @RequestBody DomainRequests.PanicResolveRequest request) {
        return panicAlertService.resolve(id, request);
    }
}
