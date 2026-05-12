ALTER TABLE diary_entries
    ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

CREATE INDEX IF NOT EXISTS idx_diary_tags
    ON diary_entries USING GIN (tags);

CREATE INDEX IF NOT EXISTS idx_diary_search_es
    ON diary_entries USING GIN (
        to_tsvector('spanish', coalesce(title, '') || ' ' || content)
    );
