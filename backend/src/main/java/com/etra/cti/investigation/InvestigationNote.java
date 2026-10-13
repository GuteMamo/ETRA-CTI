package com.etra.cti.investigation;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "investigation_notes")
public class InvestigationNote extends BaseEntity {

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
