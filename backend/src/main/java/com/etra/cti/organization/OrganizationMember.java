package com.etra.cti.organization;

import com.etra.cti.common.BaseEntity;
import com.etra.cti.user.UserRole;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "organization_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id","user_id"}))
public class OrganizationMember extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
