-- V16__init_application_api_keys.sql

CREATE TABLE application_api_keys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    key_prefix      VARCHAR(20) NOT NULL,
    key_hash        VARCHAR(64) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ
);

CREATE INDEX idx_app_api_keys_app    ON application_api_keys(application_id);
CREATE INDEX idx_app_api_keys_hash   ON application_api_keys(key_hash);
CREATE INDEX idx_app_api_keys_status ON application_api_keys(status);
