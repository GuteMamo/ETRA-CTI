package com.etra.cti.asset;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
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
}
