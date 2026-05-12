CREATE TABLE web_push_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    endpoint_hash VARCHAR(96) NOT NULL,
    p256dh TEXT NOT NULL,
    auth_secret TEXT NOT NULL,
    expiration_time TIMESTAMPTZ NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMPTZ NULL,
    last_success_at TIMESTAMPTZ NULL,
    last_failure_at TIMESTAMPTZ NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_web_push_subscriptions_user_endpoint UNIQUE (user_id, endpoint_hash)
);

CREATE INDEX idx_web_push_subscriptions_user_active ON web_push_subscriptions(user_id, active);
CREATE INDEX idx_web_push_subscriptions_active ON web_push_subscriptions(active) WHERE active = TRUE;

ALTER TABLE web_push_subscriptions ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON web_push_subscriptions FROM anon, authenticated;

CREATE TABLE web_push_deliveries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id UUID NOT NULL REFERENCES web_push_subscriptions(id) ON DELETE CASCADE,
    notification_type VARCHAR(64) NOT NULL,
    target_key VARCHAR(160) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    provider_status INTEGER NULL,
    error_message TEXT NULL,
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_web_push_deliveries_dedupe UNIQUE (user_id, subscription_id, notification_type, target_key)
);

CREATE INDEX idx_web_push_deliveries_user_type ON web_push_deliveries(user_id, notification_type);
CREATE INDEX idx_web_push_deliveries_subscription_created ON web_push_deliveries(subscription_id, created_at DESC);

ALTER TABLE web_push_deliveries ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON web_push_deliveries FROM anon, authenticated;
