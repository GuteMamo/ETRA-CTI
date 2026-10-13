-- V8__init_remediation_retest.sql

CREATE TABLE remediations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    finding_id      UUID NOT NULL UNIQUE REFERENCES findings(id) ON DELETE CASCADE,
    developer_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    fix_notes       TEXT,
    commit_ref      VARCHAR(255),
    started_at      TIMESTAMPTZ,
    fixed_at        TIMESTAMPTZ,
    verified_at     TIMESTAMPTZ,
    reopened_count  INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_remediations_finding   ON remediations(finding_id);
CREATE INDEX idx_remediations_status    ON remediations(status);
CREATE INDEX idx_remediations_developer ON remediations(developer_id);

CREATE TABLE retests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    finding_id      UUID NOT NULL REFERENCES findings(id) ON DELETE CASCADE,
    remediation_id  UUID REFERENCES remediations(id) ON DELETE SET NULL,
    performed_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    result          VARCHAR(20) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retests_finding     ON retests(finding_id);
CREATE INDEX idx_retests_remediation ON retests(remediation_id);
CREATE INDEX idx_retests_created     ON retests(created_at DESC);