package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;

public final class AdminRequests {

    private AdminRequests() {
    }

    public static class AdminUpdateUserRequest {
        private Role role;
        private Plan plan;
        private Boolean emailVerified;

        public AdminUpdateUserRequest() {
        }

        public AdminUpdateUserRequest(Role role, Plan plan, Boolean emailVerified) {
            this.role = role;
            this.plan = plan;
            this.emailVerified = emailVerified;
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
