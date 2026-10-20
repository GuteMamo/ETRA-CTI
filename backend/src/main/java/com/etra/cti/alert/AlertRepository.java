package com.etra.cti.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Page<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);
    Page<Alert> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId, Pageable pageable);
    Page<Alert> findByApplicationIdAndStatusOrderByCreatedAtDesc(UUID applicationId, AlertStatus status, Pageable pageable);
    Page<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Alert> findByEventId(UUID eventId);
    boolean existsByDetectionId(UUID detectionId);
}
