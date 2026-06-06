package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.Plan;
import java.time.Instant;

public final class BillingResponses {

    private BillingResponses() {
    }

    public static class BillingStatusResponse {
        private Plan plan;
        private String status;
        private Instant currentPeriodEnd;
        private boolean cancelAtPeriodEnd;
        private boolean customerPortalAvailable;
        private boolean testMode;
        private boolean billingConfigured;

        public BillingStatusResponse() {
        }

        public BillingStatusResponse(Plan plan, String status, Instant currentPeriodEnd,
                                     boolean cancelAtPeriodEnd, boolean customerPortalAvailable,
                                     boolean testMode, boolean billingConfigured) {
            this.plan = plan;
            this.status = status;
            this.currentPeriodEnd = currentPeriodEnd;
            this.cancelAtPeriodEnd = cancelAtPeriodEnd;
            this.customerPortalAvailable = customerPortalAvailable;
            this.testMode = testMode;
            this.billingConfigured = billingConfigured;
        }

        public Plan getPlan() {
            return plan;
        }

        public void setPlan(Plan plan) {
            this.plan = plan;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getCurrentPeriodEnd() {
            return currentPeriodEnd;
        }

        public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
            this.currentPeriodEnd = currentPeriodEnd;
        }

        public boolean isCancelAtPeriodEnd() {
            return cancelAtPeriodEnd;
        }

        public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
            this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        }

        public boolean isCustomerPortalAvailable() {
            return customerPortalAvailable;
        }

        public void setCustomerPortalAvailable(boolean customerPortalAvailable) {
            this.customerPortalAvailable = customerPortalAvailable;
        }

        public boolean isTestMode() {
            return testMode;
        }

        public void setTestMode(boolean testMode) {
            this.testMode = testMode;
        }

        public boolean isBillingConfigured() {
            return billingConfigured;
        }

        public void setBillingConfigured(boolean billingConfigured) {
            this.billingConfigured = billingConfigured;
        }
    }

    public static class RedirectResponse {
        private String url;

        public RedirectResponse() {
        }

        public RedirectResponse(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
