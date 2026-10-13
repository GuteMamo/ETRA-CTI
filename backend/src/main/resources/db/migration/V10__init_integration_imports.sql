-- V10__init_integration_imports.sql

CREATE TABLE integration_imports (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source              VARCHAR(30) NOT NULL,
    application_id      UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    filename            VARCHAR(255),
    size_bytes          BIGINT,
    findings_created    INTEGER NOT NULL DEFAULT 0,
    findings_duplicated INTEGER NOT NULL DEFAULT 0,
    assets_created      INTEGER NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message       TEXT,
    imported_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at         TIMESTAMPTZ,
    duration_ms         BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_imports_application ON integration_imports(application_id);
CREATE INDEX idx_imports_source      ON integration_imports(source);
CREATE INDEX idx_imports_started     ON integration_imports(started_at DESC);