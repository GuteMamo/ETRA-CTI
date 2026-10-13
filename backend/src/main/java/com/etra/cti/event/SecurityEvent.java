package com.etra.cti.event;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventSource source;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(length = 10)
    private String method;

    @Column(length = 1000)
    private String path;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "user_identifier", length = 255)
    private String userIdentifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> normalized;

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }
    public EventSource getSource() { return source; }
    public void setSource(EventSource source) { this.source = source; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }
    public Map<String, Object> getNormalized() { return normalized; }
    public void setNormalized(Map<String, Object> normalized) { this.normalized = normalized; }
}
