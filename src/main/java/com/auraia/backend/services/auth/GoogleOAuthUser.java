package com.auraia.backend.services.auth;

/**
 * Datos basicos que devuelve Google despues del login.
 */
public class GoogleOAuthUser {
    private String subject;
    private String email;
    private boolean emailVerified;
    private String name;

    public GoogleOAuthUser() {
    }

    /**
     * Crea el usuario recibido desde Google.
     */
    public GoogleOAuthUser(String subject, String email, boolean emailVerified, String name) {
        this.subject = subject;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
