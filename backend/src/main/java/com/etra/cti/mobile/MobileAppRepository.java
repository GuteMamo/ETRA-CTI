package com.etra.cti.mobile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobileAppRepository extends JpaRepository<MobileApp, UUID> {

    List<MobileApp> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    List<MobileApp> findAllByOrderByCreatedAtDesc();

    Optional<MobileApp> findByApplicationIdAndSha256(UUID applicationId, String sha256);

    boolean existsByApplicationIdAndSha256(UUID applicationId, String sha256);
}
