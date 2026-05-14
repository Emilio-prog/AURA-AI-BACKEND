package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.AchievementRequests;
import com.auraia.backend.models.dto.response.AchievementResponses;
import com.auraia.backend.services.achievement.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/achievements")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "List motivational achievements")
    @GetMapping
    public AchievementResponses.AchievementListResponse list() {
        return achievementService.list();
    }

    @Operation(summary = "Register an achievement event")
    @PostMapping("/events")
    public AchievementResponses.AchievementListResponse recordEvent(
        @Valid @RequestBody AchievementRequests.EventRequest request
    ) {
        return achievementService.recordEvent(request);
    }
}
