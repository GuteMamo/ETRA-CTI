package com.etra.cti.finding;

import com.etra.cti.common.BaseEntity;
import com.etra.cti.detection.Severity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "findings")
public class Finding extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "detection_id")
    private UUID detectionId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String endpoint;

    @Column(length = 255)
    private String parameter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FindingStatus status = FindingStatus.OPEN;

    @Column(length = 40)
    private String source;

    @Column(name = "source_display", length = 100)
    private String sourceDisplay;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "risk_score", precision = 6, scale = 3)
    private BigDecimal riskScore;

    @Column(name = "risk_band", length = 10)
    private String riskBand;

    @Column(name = "created_by")
    private UUID createdBy;

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getDetectionId() { return detectionId; }
    public void setDetectionId(UUID detectionId) { this.detectionId = detectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getParameter() { return parameter; }
    public void setParameter(String parameter) { this.parameter = parameter; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public FindingStatus getStatus() { return status; }
    public void setStatus(FindingStatus status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceDisplay() { return sourceDisplay; }
    public void setSourceDisplay(String sourceDisplay) { this.sourceDisplay = sourceDisplay; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public String getRiskBand() { return riskBand; }
    public void setRiskBand(String riskBand) { this.riskBand = riskBand; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
