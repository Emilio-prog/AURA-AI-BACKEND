CREATE TABLE user_achievements (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL,
    progress_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_achievements_user_code UNIQUE (user_id, code)
);

CREATE INDEX idx_user_achievements_user_unlocked ON user_achievements(user_id, unlocked_at DESC);
CREATE INDEX idx_user_achievements_code ON user_achievements(code);

ALTER TABLE user_achievements ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON user_achievements FROM anon, authenticated;

CREATE TABLE achievement_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_achievement_events_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_achievement_events_user_type ON achievement_events(user_id, event_type);
CREATE INDEX idx_achievement_events_user_occurred ON achievement_events(user_id, occurred_at DESC);

ALTER TABLE achievement_events ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON achievement_events FROM anon, authenticated;
