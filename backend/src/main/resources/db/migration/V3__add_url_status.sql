ALTER TABLE short_url
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE short_url
SET status = CASE
    WHEN expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP THEN 'EXPIRED'
    WHEN active = FALSE THEN 'INACTIVE'
    ELSE 'ACTIVE'
END
WHERE status IS NULL;

ALTER TABLE short_url
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE short_url
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_short_url_status ON short_url(status);
