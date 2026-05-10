package com.auraia.backend.services.auth;

import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.UserResponses;

public interface AuthService {

    AuthResponses.PendingVerificationResponse register(AuthRequests.RegisterRequest request);

    AuthResponses.AuthResponse login(AuthRequests.LoginRequest request);

    AuthResponses.AuthResponse refresh(AuthRequests.RefreshTokenRequest request);

    AuthResponses.MessageResponse logout(AuthRequests.LogoutRequest request);

    AuthResponses.MessageResponse verifyEmail(String token);

    AuthResponses.MessageResponse resendVerification(AuthRequests.ResendVerificationRequest request);

    AuthResponses.MessageResponse forgotPassword(AuthRequests.ForgotPasswordRequest request);

    AuthResponses.MessageResponse resetPassword(AuthRequests.ResetPasswordRequest request);

    UserResponses.UserResponse me();
}
