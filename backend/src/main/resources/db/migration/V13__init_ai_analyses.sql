-- V13__init_ai_analyses.sql

CREATE TABLE ai_analyses (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    analysis_type   VARCHAR(40) NOT NULL,
    resource_type   VARCHAR(40) NOT NULL,
    resource_id     VARCHAR(80) NOT NULL,
    prompt_hash     VARCHAR(64) NOT NULL,
    result_text     TEXT NOT NULL,
    model           VARCHAR(80),
    tokens_used     INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_type_resource ON ai_analyses(analysis_type, resource_type, resource_id);
CREATE INDEX idx_ai_hash          ON ai_analyses(prompt_hash);