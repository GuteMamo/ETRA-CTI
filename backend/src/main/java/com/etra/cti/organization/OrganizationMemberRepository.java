package com.etra.cti.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUserId(UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID orgId, UUID userId);

    @Query("SELECT m FROM OrganizationMember m WHERE m.organizationId = :orgId")
    List<OrganizationMember> findByOrganizationId(UUID orgId);
}
