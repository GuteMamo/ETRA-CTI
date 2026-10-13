package com.etra.cti.investigation;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "investigations")
public class Investigation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvestigationStatus status = InvestigationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestigationPriority priority = InvestigationPriority.MEDIUM;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_by")
    private UUID createdBy;

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public InvestigationStatus getStatus() { return status; }
    public void setStatus(InvestigationStatus status) { this.status = status; }

    public InvestigationPriority getPriority() { return priority; }
    public void setPriority(InvestigationPriority priority) { this.priority = priority; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
