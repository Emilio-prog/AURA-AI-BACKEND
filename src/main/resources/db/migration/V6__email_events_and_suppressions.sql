CREATE TABLE IF NOT EXISTS email_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    resend_email_id VARCHAR(64),
    payload TEXT,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_events_recipient ON email_events(recipient_email);
CREATE INDEX IF NOT EXISTS idx_email_events_type_received ON email_events(event_type, received_at DESC);

CREATE TABLE IF NOT EXISTS email_suppressions (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_suppressions_email ON email_suppressions(email);

ALTER TABLE email_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_suppressions ENABLE ROW LEVEL SECURITY;
