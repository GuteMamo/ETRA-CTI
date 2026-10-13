package com.etra.cti.remediation;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "retests")
public class Retest extends BaseEntity {

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(name = "remediation_id")
    private UUID remediationId;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetestResult result;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public UUID getFindingId() { return findingId; }
    public void setFindingId(UUID findingId) { this.findingId = findingId; }

    public UUID getRemediationId() { return remediationId; }
    public void setRemediationId(UUID remediationId) { this.remediationId = remediationId; }

    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }

    public RetestResult getResult() { return result; }
    public void setResult(RetestResult result) { this.result = result; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
