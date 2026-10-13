-- V4__init_findings.sql

CREATE TABLE findings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference       VARCHAR(30) NOT NULL UNIQUE,
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    detection_id    UUID REFERENCES detections(id) ON DELETE SET NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    endpoint        VARCHAR(1000),
    parameter       VARCHAR(255),
    severity        VARCHAR(10) NOT NULL,
    confidence      NUMERIC(4,3),
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    source          VARCHAR(40),
    assigned_to     UUID REFERENCES users(id) ON DELETE SET NULL,
    risk_score      NUMERIC(6,3),
    risk_band       VARCHAR(10),
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_findings_application ON findings(application_id);
CREATE INDEX idx_findings_status      ON findings(status);
CREATE INDEX idx_findings_severity    ON findings(severity);
CREATE INDEX idx_findings_risk        ON findings(risk_score DESC);
CREATE INDEX idx_findings_assigned    ON findings(assigned_to);

CREATE SEQUENCE findings_reference_seq START 1;