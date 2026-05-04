package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
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

    @Operation(summary = "Change current user password")
    @PutMapping("/me/password")
    public AuthResponses.MessageResponse changePassword(@Valid @RequestBody UserRequests.ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

    @Operation(summary = "Soft delete and anonymize current account")
    @DeleteMapping("/me")
    public AuthResponses.MessageResponse deleteMe() {
        return userService.deleteCurrentAccount();
    }

    @Operation(summary = "Export current user data as JSON")
    @GetMapping("/me/export")
    public UserResponses.ExportDataResponse exportMe() {
        return userService.exportCurrentUserData();
    }
}
