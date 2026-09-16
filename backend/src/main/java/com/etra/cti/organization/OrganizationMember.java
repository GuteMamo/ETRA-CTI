package com.etra.cti.organization;

import com.etra.cti.common.BaseEntity;
import com.etra.cti.user.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organization_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id","user_id"}))
@Getter
@Setter
public class OrganizationMember extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;
}
