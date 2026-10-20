-- V17__lifecycle_and_governance_enhancements.sql

-- Enhance findings table with remediation due dates and priority overrides
ALTER TABLE findings ADD COLUMN IF NOT EXISTS due_date TIMESTAMPTZ;
ALTER TABLE findings ADD COLUMN IF NOT EXISTS priority_override_reason TEXT;

-- Enhance alerts table with triage disposition tracking
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS disposition_notes TEXT;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS disposition_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS disposition_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_findings_due_date ON findings(due_date);
CREATE INDEX IF NOT EXISTS idx_alerts_disposition_by ON alerts(disposition_by);
