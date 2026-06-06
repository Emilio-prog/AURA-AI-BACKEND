package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    public static class RegisterRequest {
        @NotBlank
        @Size(min = 2, max = 160)
        private String name;

        @NotBlank
        @Email
        @Size(max = 320)
        private String email;

        @NotBlank
        @Size(min = 12, max = 128)
        private String password;

        private String captchaToken;

        public RegisterRequest() {
        }

        public RegisterRequest(String name, String email, String password, String captchaToken) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.captchaToken = captchaToken;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCaptchaToken() {
            return captchaToken;
        }

        public void setCaptchaToken(String captchaToken) {
            this.captchaToken = captchaToken;
        }
    }

    public static class LoginRequest {
        @NotBlank
        @Email
        @Size(max = 320)
        private String email;

        @NotBlank
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;

        public RefreshTokenRequest() {
        }

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class LogoutRequest {
        @NotBlank
        private String refreshToken;

        public LogoutRequest() {
        }

        public LogoutRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class ResendVerificationRequest {
        @NotBlank
        @Email
        @Size(max = 320)
        private String email;

        public ResendVerificationRequest() {
        }

        public ResendVerificationRequest(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ForgotPasswordRequest {
        @NotBlank
        @Email
        @Size(max = 320)
        private String email;

        private String captchaToken;

        public ForgotPasswordRequest() {
        }

        public ForgotPasswordRequest(String email, String captchaToken) {
            this.email = email;
            this.captchaToken = captchaToken;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCaptchaToken() {
            return captchaToken;
        }

        public void setCaptchaToken(String captchaToken) {
            this.captchaToken = captchaToken;
        }
    }

    public static class ResetPasswordRequest {
        @NotBlank
        private String token;

        @NotBlank
        @Size(min = 12, max = 128)
        private String password;

        public ResetPasswordRequest() {
        }

        public ResetPasswordRequest(String token, String password) {
            this.token = token;
            this.password = password;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class OAuthExchangeRequest {
        @NotBlank
        private String code;

        public OAuthExchangeRequest() {
        }

        public OAuthExchangeRequest(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
