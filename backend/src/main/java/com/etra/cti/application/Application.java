package com.etra.cti.application;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
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
}
