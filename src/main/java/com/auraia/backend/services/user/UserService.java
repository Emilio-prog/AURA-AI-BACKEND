package com.auraia.backend.services.user;

import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;

public interface UserService {

    UserResponses.UserResponse getCurrentProfile();

    UserResponses.UserResponse updateCurrentProfile(UserRequests.UpdateUserRequest request);

    AuthResponses.MessageResponse changePassword(UserRequests.ChangePasswordRequest request);

    AuthResponses.MessageResponse deleteCurrentAccount();

    UserResponses.ExportDataResponse exportCurrentUserData();
}
