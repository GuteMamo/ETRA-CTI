-- V12__init_threat_intel.sql

CREATE TABLE iocs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type            VARCHAR(20) NOT NULL,
    value           VARCHAR(500) NOT NULL,
    description     TEXT,
    source          VARCHAR(100),
    severity        VARCHAR(10) NOT NULL DEFAULT 'HIGH',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMPTZ,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (type, value)
);

CREATE INDEX idx_iocs_type  ON iocs(type);
CREATE INDEX idx_iocs_value ON iocs(value);
CREATE INDEX idx_iocs_active ON iocs(active);