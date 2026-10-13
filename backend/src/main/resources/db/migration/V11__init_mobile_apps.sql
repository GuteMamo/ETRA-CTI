-- V11__init_mobile_apps.sql

CREATE TABLE mobile_apps (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id      UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    platform            VARCHAR(20) NOT NULL,
    package_name        VARCHAR(255) NOT NULL,
    version_name        VARCHAR(100),
    version_code        VARCHAR(50),
    file_name           VARCHAR(255),
    size_bytes          BIGINT,
    sha256              VARCHAR(64) NOT NULL,
    signed              BOOLEAN,
    min_sdk             VARCHAR(20),
    target_sdk          VARCHAR(20),
    permissions         JSONB,
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (application_id, sha256)
);

CREATE INDEX idx_mobile_apps_application ON mobile_apps(application_id);
CREATE INDEX idx_mobile_apps_package     ON mobile_apps(package_name);
CREATE INDEX idx_mobile_apps_sha256      ON mobile_apps(sha256);
CREATE INDEX idx_mobile_apps_platform    ON mobile_apps(platform);