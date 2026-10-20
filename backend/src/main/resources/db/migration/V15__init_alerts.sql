-- V15__init_alerts.sql

CREATE TABLE alerts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference       VARCHAR(30) NOT NULL UNIQUE,
    detection_id    UUID REFERENCES detections(id) ON DELETE CASCADE,
    event_id        UUID REFERENCES security_events(id) ON DELETE CASCADE,
    application_id  UUID REFERENCES applications(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    severity        VARCHAR(10) NOT NULL,
    confidence      NUMERIC(4,3),
    risk_score      NUMERIC(6,3),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by UUID REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE SEQUENCE alerts_reference_seq START 1;

CREATE INDEX idx_alerts_application ON alerts(application_id);
CREATE INDEX idx_alerts_status      ON alerts(status);
CREATE INDEX idx_alerts_severity    ON alerts(severity);
CREATE INDEX idx_alerts_created_at  ON alerts(created_at DESC);
