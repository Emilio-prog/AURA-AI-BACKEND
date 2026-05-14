ALTER TABLE public.users
    ADD COLUMN onboarded_at TIMESTAMPTZ,
    ADD COLUMN onboarding_consent_at TIMESTAMPTZ,
    ADD COLUMN onboarding_consent_version VARCHAR(120),
    ADD COLUMN onboarding_profile JSONB NOT NULL DEFAULT '{}'::jsonb;
