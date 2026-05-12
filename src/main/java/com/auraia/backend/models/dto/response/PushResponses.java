package com.auraia.backend.models.dto.response;

public final class PushResponses {

    private PushResponses() {
    }

    public record PushConfigResponse(
        boolean enabled,
        String publicKey,
        boolean subscribed
    ) {
    }

    public record PushSubscriptionResponse(
        boolean subscribed
    ) {
    }

    public record PushTestResponse(
        boolean sent
    ) {
    }
}
