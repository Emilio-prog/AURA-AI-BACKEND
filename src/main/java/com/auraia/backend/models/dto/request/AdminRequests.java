package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;

/**
 * Peticiones que usa la zona de administración.
 * De momento sirve para cambiar datos básicos de un usuario.
 */
public final class AdminRequests {

    /**
     * Evita crear objetos de esta clase contenedora.
     */
    private AdminRequests() {
    }

    /**
     * Comprueba que el administrador haya enviado al menos un cambio.
     */
    private static void validarUnCampo(Role role, Plan plan, Boolean emailVerified) {
        if (role == null && plan == null && emailVerified == null) {
            throw new IllegalArgumentException("Debe indicarse algun dato para actualizar");
        }
    }

    /**
     * Datos que un administrador puede cambiar de un usuario.
     */
    public static class AdminUpdateUserRequest {
        private Role role;
        private Plan plan;
        private Boolean emailVerified;

        /**
         * Constructor vacio usado por Spring al recibir JSON.
         */
        public AdminUpdateUserRequest() {
        }

        /**
         * Crea la petición y revisa que venga algún dato para actualizar.
         */
        public AdminUpdateUserRequest(Role role, Plan plan, Boolean emailVerified) {
            this.role = role;
            this.plan = plan;
            this.emailVerified = emailVerified;
            validarUnCampo(this.role, this.plan, this.emailVerified);
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Plan getPlan() {
            return plan;
        }

        public void setPlan(Plan plan) {
            this.plan = plan;
        }

        public Boolean getEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
        }
    }
}
