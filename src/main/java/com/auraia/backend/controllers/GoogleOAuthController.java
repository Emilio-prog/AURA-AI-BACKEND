package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.services.auth.GoogleOAuthService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/oauth/google")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/start")
    public AuthResponses.OAuthStartResponse startLogin() {
        return googleOAuthService.startLogin();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String error) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(googleOAuthService.handleCallback(state, code, error)))
            .build();
    }

    @PostMapping("/exchange")
    public AuthResponses.AuthResponse exchange(@Valid @RequestBody AuthRequests.OAuthExchangeRequest request) {
        return googleOAuthService.exchange(request);
    }

    @GetMapping("/status")
    public AuthResponses.OAuthStatusResponse status() {
        return googleOAuthService.status();
    }

    @PostMapping("/link/start")
    public AuthResponses.OAuthStartResponse startLink() {
        return googleOAuthService.startLink();
    }

    @DeleteMapping("/link")
    public AuthResponses.MessageResponse unlink() {
        return googleOAuthService.unlink();
    }
}
