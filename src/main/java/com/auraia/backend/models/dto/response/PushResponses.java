package com.auraia.backend.models.dto.response;

public final class PushResponses {

    private PushResponses() {
    }

    public static class PushConfigResponse {
        private boolean enabled;
        private String publicKey;
        private boolean subscribed;

        public PushConfigResponse() {
        }

        public PushConfigResponse(boolean enabled, String publicKey, boolean subscribed) {
            this.enabled = enabled;
            this.publicKey = publicKey;
            this.subscribed = subscribed;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public boolean isSubscribed() {
            return subscribed;
        }

        public void setSubscribed(boolean subscribed) {
            this.subscribed = subscribed;
        }
    }

    public static class PushSubscriptionResponse {
        private boolean subscribed;

        public PushSubscriptionResponse() {
        }

        public PushSubscriptionResponse(boolean subscribed) {
            this.subscribed = subscribed;
        }

        public boolean isSubscribed() {
            return subscribed;
        }

        public void setSubscribed(boolean subscribed) {
            this.subscribed = subscribed;
        }
    }

    public static class PushTestResponse {
        private boolean sent;

        public PushTestResponse() {
        }

        public PushTestResponse(boolean sent) {
            this.sent = sent;
        }

        public boolean isSent() {
            return sent;
        }

        public void setSent(boolean sent) {
            this.sent = sent;
        }
    }
}
