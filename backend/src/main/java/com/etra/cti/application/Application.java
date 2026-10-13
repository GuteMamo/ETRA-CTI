package com.etra.cti.application;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Criticality criticality;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "owner_id")
    private UUID ownerId;

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ApplicationType getType() { return type; }
    public void setType(ApplicationType type) { this.type = type; }

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public Criticality getCriticality() { return criticality; }
    public void setCriticality(Criticality criticality) { this.criticality = criticality; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
}
