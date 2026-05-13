ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    linked_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX idx_oauth_identities_provider_subject_active
    ON oauth_identities(provider, provider_subject)
    WHERE active = TRUE;

CREATE UNIQUE INDEX idx_oauth_identities_user_provider_active
    ON oauth_identities(user_id, provider)
    WHERE active = TRUE;

CREATE INDEX idx_oauth_identities_user ON oauth_identities(user_id);

ALTER TABLE oauth_identities ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON oauth_identities FROM anon, authenticated;

CREATE TABLE oauth_states (
    id UUID PRIMARY KEY,
    user_id UUID NULL REFERENCES users(id) ON DELETE CASCADE,
    state_hash VARCHAR(96) NOT NULL UNIQUE,
    flow VARCHAR(24) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_oauth_states_user_created ON oauth_states(user_id, created_at DESC);
CREATE INDEX idx_oauth_states_expires ON oauth_states(expires_at);

ALTER TABLE oauth_states ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON oauth_states FROM anon, authenticated;

CREATE TABLE oauth_exchange_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(96) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_oauth_exchange_codes_user_created ON oauth_exchange_codes(user_id, created_at DESC);
CREATE INDEX idx_oauth_exchange_codes_expires ON oauth_exchange_codes(expires_at);

ALTER TABLE oauth_exchange_codes ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON oauth_exchange_codes FROM anon, authenticated;
