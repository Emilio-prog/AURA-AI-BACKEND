package com.auraia.backend.models.dto.request;

import com.auraia.backend.models.enums.Plan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public final class BillingRequests {

    private BillingRequests() {
    }

    public record CheckoutRequest(
        @NotNull Plan plan
    ) {
    }

    public record CheckoutSyncRequest(
        @NotBlank String sessionId
    ) {
    }
}
