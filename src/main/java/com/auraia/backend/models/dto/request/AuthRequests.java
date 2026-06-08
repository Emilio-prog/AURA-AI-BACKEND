package com.auraia.backend.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    private static void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio");
        }
    }

    private static void validarLongitud(String valor, String campo, int minimo, int maximo) {
        if (valor == null) {
            return;
        }
        if (valor.length() < minimo || valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " tiene una longitud incorrecta");
        }
    }

    private static void validarEmail(String email) {
        validarTextoObligatorio(email, "email");
        validarLongitud(email, "email", 1, 320);
        if (!email.contains("@")) {
            throw new IllegalArgumentException("email no tiene un formato valido");
        }
    }

    public static class RegisterRequest {
        @NotBlank(message = "name no puede estar vacio")
        @Size(min = 2, max = 160, message = "name tiene una longitud incorrecta")
        private String name;

        @NotBlank(message = "email no puede estar vacio")
        @Email(message = "email no tiene un formato valido")
        @Size(max = 320, message = "email tiene una longitud incorrecta")
        private String email;

        @NotBlank(message = "password no puede estar vacio")
        @Size(min = 12, max = 128, message = "password tiene una longitud incorrecta")
        private String password;

        private String captchaToken;

        public RegisterRequest() {
        }

        public RegisterRequest(String name, String email, String password, String captchaToken) {
            this.name = name;
            this.email = email;
            this.password = password;
            this.captchaToken = captchaToken;
            validarTextoObligatorio(this.name, "name");
            validarLongitud(this.name, "name", 2, 160);
            validarEmail(this.email);
            validarTextoObligatorio(this.password, "password");
            validarLongitud(this.password, "password", 12, 128);
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
        @NotBlank(message = "email no puede estar vacio")
        @Email(message = "email no tiene un formato valido")
        @Size(max = 320, message = "email tiene una longitud incorrecta")
        private String email;

        @NotBlank(message = "password no puede estar vacio")
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
            validarEmail(this.email);
            validarTextoObligatorio(this.password, "password");
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
        @NotBlank(message = "refreshToken no puede estar vacio")
        private String refreshToken;

        public RefreshTokenRequest() {
        }

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
            validarTextoObligatorio(this.refreshToken, "refreshToken");
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class LogoutRequest {
        @NotBlank(message = "refreshToken no puede estar vacio")
        private String refreshToken;

        public LogoutRequest() {
        }

        public LogoutRequest(String refreshToken) {
            this.refreshToken = refreshToken;
            validarTextoObligatorio(this.refreshToken, "refreshToken");
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class ResendVerificationRequest {
        @NotBlank(message = "email no puede estar vacio")
        @Email(message = "email no tiene un formato valido")
        @Size(max = 320, message = "email tiene una longitud incorrecta")
        private String email;

        public ResendVerificationRequest() {
        }

        public ResendVerificationRequest(String email) {
            this.email = email;
            validarEmail(this.email);
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "email no puede estar vacio")
        @Email(message = "email no tiene un formato valido")
        @Size(max = 320, message = "email tiene una longitud incorrecta")
        private String email;

        private String captchaToken;

        public ForgotPasswordRequest() {
        }

        public ForgotPasswordRequest(String email, String captchaToken) {
            this.email = email;
            this.captchaToken = captchaToken;
            validarEmail(this.email);
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
        @NotBlank(message = "token no puede estar vacio")
        private String token;

        @NotBlank(message = "password no puede estar vacio")
        @Size(min = 12, max = 128, message = "password tiene una longitud incorrecta")
        private String password;

        public ResetPasswordRequest() {
        }

        public ResetPasswordRequest(String token, String password) {
            this.token = token;
            this.password = password;
            validarTextoObligatorio(this.token, "token");
            validarTextoObligatorio(this.password, "password");
            validarLongitud(this.password, "password", 12, 128);
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
        @NotBlank(message = "code no puede estar vacio")
        private String code;

        public OAuthExchangeRequest() {
        }

        public OAuthExchangeRequest(String code) {
            this.code = code;
            validarTextoObligatorio(this.code, "code");
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
