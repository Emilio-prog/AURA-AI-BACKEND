package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "202", description = "User created and email verification sent")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthResponses.PendingVerificationResponse register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public AuthResponses.AuthResponse login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Rotate refresh token and return a new access token")
    @PostMapping("/refresh")
    public AuthResponses.AuthResponse refresh(@Valid @RequestBody AuthRequests.RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Revoke the current refresh token")
    @PostMapping("/logout")
    public AuthResponses.MessageResponse logout(@Valid @RequestBody AuthRequests.LogoutRequest request) {
        return authService.logout(request);
    }

    @Operation(summary = "Verify an email address")
    @PostMapping("/verify-email")
    public AuthResponses.MessageResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @Operation(summary = "Resend verification email")
    @PostMapping("/resend-verification")
    public AuthResponses.MessageResponse resendVerification(@Valid @RequestBody AuthRequests.ResendVerificationRequest request) {
        return authService.resendVerification(request);
    }

    @Operation(summary = "Return authenticated user")
    @GetMapping("/me")
    public UserResponses.UserResponse me() {
        return authService.me();
    }
}
