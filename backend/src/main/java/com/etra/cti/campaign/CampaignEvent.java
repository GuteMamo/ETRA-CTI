package com.etra.cti.campaign;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "campaign_events")
public class CampaignEvent extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AttackStage stage;

    @Column(name = "detection_type", length = 60)
    private String detectionType;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public AttackStage getStage() { return stage; }
    public void setStage(AttackStage stage) { this.stage = stage; }

    public String getDetectionType() { return detectionType; }
    public void setDetectionType(String detectionType) { this.detectionType = detectionType; }

    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
}
