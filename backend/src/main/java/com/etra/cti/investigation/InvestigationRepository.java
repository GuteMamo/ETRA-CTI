package com.etra.cti.investigation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRepository extends JpaRepository<Investigation, UUID>, JpaSpecificationExecutor<Investigation> {

    List<Investigation> findAllByOrderByCreatedAtDesc();

    List<Investigation> findByStatusOrderByCreatedAtDesc(InvestigationStatus status);

    List<Investigation> findByAssignedToOrderByCreatedAtDesc(UUID assignedTo);

    Optional<Investigation> findByCampaignId(UUID campaignId);

    List<Investigation> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
