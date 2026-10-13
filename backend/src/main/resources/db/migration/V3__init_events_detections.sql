-- V3__init_events_detections.sql

CREATE TABLE security_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    source          VARCHAR(40) NOT NULL,
    event_time      TIMESTAMPTZ NOT NULL,
    source_ip       VARCHAR(45),
    method          VARCHAR(10),
    path            VARCHAR(1000),
    status_code     INTEGER,
    user_identifier VARCHAR(255),
    raw_payload     JSONB,
    normalized      JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_application_time ON security_events(application_id, event_time DESC);
CREATE INDEX idx_events_source_ip        ON security_events(source_ip);
CREATE INDEX idx_events_normalized_gin   ON security_events USING GIN (normalized);

CREATE TABLE detections (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id        UUID NOT NULL REFERENCES security_events(id) ON DELETE CASCADE,
    rule_id         VARCHAR(80) NOT NULL,
    type            VARCHAR(60) NOT NULL,
    severity        VARCHAR(10) NOT NULL,
    confidence      NUMERIC(4,3) NOT NULL,
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_detections_event ON detections(event_id);
CREATE INDEX idx_detections_type  ON detections(type);