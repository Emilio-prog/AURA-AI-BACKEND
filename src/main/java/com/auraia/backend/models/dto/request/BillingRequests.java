package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Plan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public final class BillingRequests {

    private BillingRequests() {
    }

    public static class CheckoutRequest {
        @NotNull
        private Plan plan;

        public CheckoutRequest() {
        }

        public CheckoutRequest(Plan plan) {
            this.plan = plan;
        }

        public Plan getPlan() {
            return plan;
        }

        public void setPlan(Plan plan) {
            this.plan = plan;
        }
    }

    public static class CheckoutSyncRequest {
        @NotBlank
        private String sessionId;

        public CheckoutSyncRequest() {
        }

        public CheckoutSyncRequest(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
