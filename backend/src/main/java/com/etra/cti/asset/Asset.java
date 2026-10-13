package com.etra.cti.asset;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetType type;

    @Column(length = 255)
    private String hostname;

    @Column(length = 1000)
    private String url;

    private Integer port;
    private String protocol;
    private String environment;
    private String criticality;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getCriticality() { return criticality; }
    public void setCriticality(String criticality) { this.criticality = criticality; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
