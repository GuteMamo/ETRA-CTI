package com.etra.cti.investigation;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "investigation_findings")
public class InvestigationFinding extends BaseEntity {

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public UUID getFindingId() { return findingId; }
    public void setFindingId(UUID findingId) { this.findingId = findingId; }
}
