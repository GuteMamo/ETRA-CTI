-- V5__init_evidence.sql

CREATE TABLE evidence (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    finding_id      UUID NOT NULL REFERENCES findings(id) ON DELETE CASCADE,
    filename        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100),
    size_bytes      BIGINT,
    storage_path    VARCHAR(500) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_finding ON evidence(finding_id);
CREATE INDEX idx_evidence_sha256  ON evidence(sha256);

CREATE TABLE evidence_notes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evidence_id     UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    author_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    note            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_notes_evidence ON evidence_notes(evidence_id);