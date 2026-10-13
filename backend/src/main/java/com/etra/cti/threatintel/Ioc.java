package com.etra.cti.threatintel;

import com.etra.cti.common.BaseEntity;
import com.etra.cti.detection.Severity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iocs")
public class Ioc extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IocType type;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity = Severity.HIGH;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    public IocType getType() { return type; }
    public void setType(IocType type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
