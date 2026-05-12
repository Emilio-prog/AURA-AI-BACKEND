ALTER TABLE diary_entries
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN mood_label TYPE TEXT,
    ADD COLUMN IF NOT EXISTS search_tokens TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

ALTER TABLE chat_sessions
    ALTER COLUMN title TYPE TEXT;

ALTER TABLE contacts
    ALTER COLUMN name TYPE TEXT,
    ALTER COLUMN phone TYPE TEXT,
    ALTER COLUMN relationship TYPE TEXT;

ALTER TABLE panic_notification_results
    ALTER COLUMN details TYPE TEXT;

DROP INDEX IF EXISTS idx_diary_search_es;

CREATE INDEX IF NOT EXISTS idx_diary_search_tokens
    ON diary_entries USING GIN (search_tokens);
