package com.auraia.backend.models.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class PushRequests {

    private PushRequests() {
    }

    public static class SubscriptionRequest {
        @NotBlank
        private String endpoint;

        private Instant expirationTime;

        @Valid
        @NotNull
        private SubscriptionKeys keys;

        public SubscriptionRequest() {
        }

        public SubscriptionRequest(String endpoint, Instant expirationTime, SubscriptionKeys keys) {
            this.endpoint = endpoint;
            this.expirationTime = expirationTime;
            this.keys = keys;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Instant getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(Instant expirationTime) {
            this.expirationTime = expirationTime;
        }

        public SubscriptionKeys getKeys() {
            return keys;
        }

        public void setKeys(SubscriptionKeys keys) {
            this.keys = keys;
        }
    }

    public static class SubscriptionKeys {
        @NotBlank
        private String p256dh;

        @NotBlank
        private String auth;

        public SubscriptionKeys() {
        }

        public SubscriptionKeys(String p256dh, String auth) {
            this.p256dh = p256dh;
            this.auth = auth;
        }

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }

    public static class DisableSubscriptionRequest {
        private String endpoint;

        public DisableSubscriptionRequest() {
        }

        public DisableSubscriptionRequest(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
