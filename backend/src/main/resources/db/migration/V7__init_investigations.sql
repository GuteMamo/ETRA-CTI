-- V7__init_investigations.sql

CREATE TABLE investigations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference       VARCHAR(30) NOT NULL UNIQUE,
    campaign_id     UUID REFERENCES attack_campaigns(id) ON DELETE SET NULL,
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assigned_to     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_investigations_campaign     ON investigations(campaign_id);
CREATE INDEX idx_investigations_application  ON investigations(application_id);
CREATE INDEX idx_investigations_status       ON investigations(status);
CREATE INDEX idx_investigations_assigned     ON investigations(assigned_to);

CREATE TABLE investigation_findings (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    investigation_id  UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    finding_id        UUID NOT NULL REFERENCES findings(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (investigation_id, finding_id)
);

CREATE INDEX idx_inv_findings_inv     ON investigation_findings(investigation_id);
CREATE INDEX idx_inv_findings_finding ON investigation_findings(finding_id);

CREATE TABLE investigation_notes (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    investigation_id  UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    author_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    note              TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_notes_inv ON investigation_notes(investigation_id);