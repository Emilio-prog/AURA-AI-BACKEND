package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.AdminRequests;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.dto.response.UserResponses;
import com.auraia.backend.services.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@ApiResponse(responseCode = "403", description = "Admin role required")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "List users as admin")
    @GetMapping
    public PageResponse<UserResponses.AdminUserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return adminUserService.listUsers(pageable);
    }

    @Operation(summary = "Get user detail as admin")
    @GetMapping("/{id}")
    public UserResponses.AdminUserResponse get(@PathVariable UUID id) {
        return adminUserService.getUser(id);
    }

    @Operation(summary = "Update user role, plan or email verification as admin")
    @PutMapping("/{id}")
    public UserResponses.AdminUserResponse update(@PathVariable UUID id,
                                                  @Valid @RequestBody AdminRequests.AdminUpdateUserRequest request) {
        return adminUserService.updateUser(id, request);
    }

    @Operation(summary = "Soft delete user as admin")
    @DeleteMapping("/{id}")
    public UserResponses.AdminUserResponse delete(@PathVariable UUID id) {
        return adminUserService.softDeleteUser(id);
    }

    @Operation(summary = "Restore a soft-deleted user as admin")
    @PutMapping("/{id}/restore")
    public UserResponses.AdminUserResponse restore(@PathVariable UUID id) {
        return adminUserService.restoreUser(id);
    }
}
