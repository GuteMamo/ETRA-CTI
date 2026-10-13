-- V6__init_campaigns.sql

CREATE TABLE attack_campaigns (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference       VARCHAR(30) NOT NULL UNIQUE,
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    source_ip       VARCHAR(45),
    title           VARCHAR(255) NOT NULL,
    risk_band       VARCHAR(10),
    event_count     INTEGER NOT NULL DEFAULT 0,
    first_seen      TIMESTAMPTZ NOT NULL,
    last_seen       TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_application ON attack_campaigns(application_id);
CREATE INDEX idx_campaigns_source_ip   ON attack_campaigns(source_ip);
CREATE INDEX idx_campaigns_last_seen   ON attack_campaigns(last_seen DESC);

CREATE TABLE campaign_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_id     UUID NOT NULL REFERENCES attack_campaigns(id) ON DELETE CASCADE,
    event_id        UUID NOT NULL REFERENCES security_events(id) ON DELETE CASCADE,
    stage           VARCHAR(40) NOT NULL,
    detection_type  VARCHAR(60),
    sequence_no     INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (campaign_id, event_id)
);

CREATE INDEX idx_campaign_events_campaign ON campaign_events(campaign_id);
CREATE INDEX idx_campaign_events_sequence ON campaign_events(campaign_id, sequence_no);