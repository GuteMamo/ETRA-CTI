package com.etra.cti.campaign;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attack_campaigns")
public class AttackCampaign extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "risk_band", length = 10)
    private String riskBand;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount = 0;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRiskBand() { return riskBand; }
    public void setRiskBand(String riskBand) { this.riskBand = riskBand; }

    public Integer getEventCount() { return eventCount; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }

    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
