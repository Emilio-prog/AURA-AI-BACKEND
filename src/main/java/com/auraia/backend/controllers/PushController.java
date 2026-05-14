package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.PushRequests;
import com.auraia.backend.models.dto.response.PushResponses;
import com.auraia.backend.services.push.WebPushService;
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
@RequestMapping("/api/v1/push")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class PushController {

    private final WebPushService webPushService;

    @Operation(summary = "Get Web Push configuration")
    @GetMapping("/config")
    public PushResponses.PushConfigResponse config() {
        return webPushService.config();
    }

    @Operation(summary = "Register Web Push subscription")
    @PostMapping("/subscriptions")
    public PushResponses.PushSubscriptionResponse subscribe(@Valid @RequestBody PushRequests.SubscriptionRequest request) {
        return webPushService.subscribe(request);
    }

    @Operation(summary = "Disable Web Push subscription")
    @PostMapping("/subscriptions/disable")
    public PushResponses.PushSubscriptionResponse disable(@RequestBody(required = false) PushRequests.DisableSubscriptionRequest request) {
        return webPushService.disable(request == null ? new PushRequests.DisableSubscriptionRequest(null) : request);
    }

    @Operation(summary = "Send Web Push test notification")
    @PostMapping("/test")
    public PushResponses.PushTestResponse test() {
        return webPushService.test();
    }
}
