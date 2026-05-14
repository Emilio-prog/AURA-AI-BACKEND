package com.auraia.backend.controllers;

import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@ApiResponse(responseCode = "401", description = "Authentication required")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public UserResponses.UserResponse me() {
        return userService.getCurrentProfile();
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/me")
    public UserResponses.UserResponse updateMe(@Valid @RequestBody UserRequests.UpdateUserRequest request) {
        return userService.updateCurrentProfile(request);
    }

    @Operation(summary = "Complete current user onboarding")
    @PostMapping("/me/onboarding")
    public UserResponses.UserResponse completeOnboarding(@Valid @RequestBody UserRequests.CompleteOnboardingRequest request) {
        return userService.completeOnboarding(request);
    }

    @Operation(summary = "Change current user password")
    @PutMapping("/me/password")
    public AuthResponses.MessageResponse changePassword(@Valid @RequestBody UserRequests.ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

    @Operation(summary = "Delete current account permanently after explicit confirmation")
    @PostMapping("/me/delete")
    public AuthResponses.MessageResponse deleteMeConfirmed(@Valid @RequestBody UserRequests.DeleteAccountRequest request) {
        return userService.deleteCurrentAccount(request);
    }

    @Operation(summary = "Account deletion requires explicit confirmation")
    @DeleteMapping("/me")
    public AuthResponses.MessageResponse deleteMe() {
        throw new BusinessException("error.account_delete_confirmation_required");
    }

    @Operation(summary = "Export current user data as JSON")
    @GetMapping("/me/export")
    public UserResponses.ExportDataResponse exportMe() {
        return userService.exportCurrentUserData();
    }

    @Operation(summary = "Export current user data as PDF")
    @GetMapping(value = "/me/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportMePdf() {
        byte[] pdf = userService.exportCurrentUserDataPdf();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aura-export.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
