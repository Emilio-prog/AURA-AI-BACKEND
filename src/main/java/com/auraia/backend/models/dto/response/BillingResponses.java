package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.Plan;
import java.time.Instant;

public final class BillingResponses {

    private BillingResponses() {
    }

    public record BillingStatusResponse(
        Plan plan,
        String status,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        boolean customerPortalAvailable,
        boolean testMode,
        boolean billingConfigured
    ) {
    }

    public record RedirectResponse(
        String url
    ) {
    }
}
