package com.auraia.backend.models.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class PushRequests {

    private PushRequests() {
    }

    public record SubscriptionRequest(
        @NotBlank String endpoint,
        Instant expirationTime,
        @Valid @NotNull SubscriptionKeys keys
    ) {
    }

    public record SubscriptionKeys(
        @NotBlank String p256dh,
        @NotBlank String auth
    ) {
    }

    public record DisableSubscriptionRequest(String endpoint) {
    }
}
