package com.etra.cti.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegrationImportRepository extends JpaRepository<IntegrationImport, UUID> {

    List<IntegrationImport> findAllByOrderByStartedAtDesc();

    List<IntegrationImport> findByApplicationIdOrderByStartedAtDesc(UUID applicationId);

    List<IntegrationImport> findBySourceOrderByStartedAtDesc(IntegrationSource source);
}
