UPDATE users SET plan = 'PERSONAL' WHERE plan = 'PRO';
UPDATE users SET plan = 'PREMIUM' WHERE plan = 'TEAM';

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    stripe_customer_id VARCHAR(120) UNIQUE,
    stripe_subscription_id VARCHAR(120) UNIQUE,
    stripe_price_id VARCHAR(120),
    plan VARCHAR(32) NOT NULL DEFAULT 'FREE',
    status VARCHAR(40) NOT NULL DEFAULT 'none',
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_stripe_customer ON subscriptions(stripe_customer_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON subscriptions FROM anon, authenticated;
