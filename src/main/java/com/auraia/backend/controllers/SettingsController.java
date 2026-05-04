package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.services.settings.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settings")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "Get user settings")
    @GetMapping
    public DomainResponses.UserSettingsResponse get() {
        return settingsService.get();
    }

    @Operation(summary = "Update user settings")
    @PutMapping
    public DomainResponses.UserSettingsResponse update(@Valid @RequestBody DomainRequests.UserSettingsRequest request) {
        return settingsService.update(request);
    }
}
