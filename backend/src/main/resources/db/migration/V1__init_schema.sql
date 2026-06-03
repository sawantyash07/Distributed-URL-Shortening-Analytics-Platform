CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS short_url_sequence START WITH 100000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role user_role NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS short_url (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    short_code VARCHAR(32) NOT NULL UNIQUE,
    custom_alias BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255),
    original_url TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    click_count BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_accessed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS click_event (
    id BIGSERIAL PRIMARY KEY,
    short_url_id UUID NOT NULL REFERENCES short_url(id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    browser VARCHAR(64),
    operating_system VARCHAR(64),
    device_type VARCHAR(32),
    country VARCHAR(64),
    referrer VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_short_url_owner_id ON short_url(owner_id);
CREATE INDEX IF NOT EXISTS idx_short_url_created_at ON short_url(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_short_url_expires_at ON short_url(expires_at);
CREATE INDEX IF NOT EXISTS idx_click_event_short_url_id ON click_event(short_url_id);
CREATE INDEX IF NOT EXISTS idx_click_event_clicked_at ON click_event(clicked_at DESC);
CREATE INDEX IF NOT EXISTS idx_click_event_country ON click_event(country);
CREATE INDEX IF NOT EXISTS idx_click_event_browser ON click_event(browser);
