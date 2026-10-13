package com.etra.cti.remediation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetestRepository extends JpaRepository<Retest, UUID> {

    List<Retest> findByFindingIdOrderByCreatedAtDesc(UUID findingId);

    List<Retest> findByRemediationIdOrderByCreatedAtDesc(UUID remediationId);
}
