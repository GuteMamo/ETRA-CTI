package com.etra.cti.integration;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_imports")
public class IntegrationImport extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationSource source;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(length = 255)
    private String filename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "findings_created", nullable = false)
    private Integer findingsCreated = 0;

    @Column(name = "findings_duplicated", nullable = false)
    private Integer findingsDuplicated = 0;

    @Column(name = "assets_created", nullable = false)
    private Integer assetsCreated = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "imported_by")
    private UUID importedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public IntegrationSource getSource() { return source; }
    public void setSource(IntegrationSource source) { this.source = source; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Integer getFindingsCreated() { return findingsCreated; }
    public void setFindingsCreated(Integer findingsCreated) { this.findingsCreated = findingsCreated; }

    public Integer getFindingsDuplicated() { return findingsDuplicated; }
    public void setFindingsDuplicated(Integer findingsDuplicated) { this.findingsDuplicated = findingsDuplicated; }

    public Integer getAssetsCreated() { return assetsCreated; }
    public void setAssetsCreated(Integer assetsCreated) { this.assetsCreated = assetsCreated; }

    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public UUID getImportedBy() { return importedBy; }
    public void setImportedBy(UUID importedBy) { this.importedBy = importedBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
