-- V9__init_audit_logs.sql

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(80),
    resource_id     VARCHAR(80),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user      ON audit_logs(user_id);
CREATE INDEX idx_audit_resource  ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_action    ON audit_logs(action);
CREATE INDEX idx_audit_created   ON audit_logs(created_at DESC);