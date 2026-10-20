package com.etra.cti.finding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID>, JpaSpecificationExecutor<Finding> {

    Page<Finding> findAllByOrderByRiskScoreDesc(Pageable pageable);

    Page<Finding> findByApplicationIdOrderByRiskScoreDesc(UUID applicationId, Pageable pageable);

    List<Finding> findByStatusOrderByRiskScoreDesc(FindingStatus status);

    List<Finding> findByAssignedToOrderByRiskScoreDesc(UUID assignedTo);

    boolean existsByReference(String reference);
}
