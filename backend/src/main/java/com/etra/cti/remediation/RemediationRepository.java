package com.etra.cti.remediation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemediationRepository extends JpaRepository<Remediation, UUID> {

    Optional<Remediation> findByFindingId(UUID findingId);

    List<Remediation> findByStatusOrderByCreatedAtDesc(RemediationStatus status);

    List<Remediation> findByDeveloperIdOrderByCreatedAtDesc(UUID developerId);

    List<Remediation> findAllByOrderByCreatedAtDesc();
}
