package com.auraia.backend.services.admin;

import com.auraia.backend.models.dto.request.AdminRequests;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.dto.response.UserResponses;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PageResponse<UserResponses.AdminUserResponse> listUsers(Pageable pageable);

    UserResponses.AdminUserResponse getUser(UUID id);

    UserResponses.AdminUserResponse updateUser(UUID id, AdminRequests.AdminUpdateUserRequest request);

    UserResponses.AdminUserResponse softDeleteUser(UUID id);

    UserResponses.AdminUserResponse restoreUser(UUID id);
}
