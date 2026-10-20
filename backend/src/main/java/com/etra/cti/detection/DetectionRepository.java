package com.etra.cti.detection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionRepository extends JpaRepository<Detection, UUID> {
    List<Detection> findByEventId(UUID eventId);
    Page<Detection> findByEventId(UUID eventId, Pageable pageable);
    Page<Detection> findByType(String type, Pageable pageable);
    Page<Detection> findBySeverity(Severity severity, Pageable pageable);
    Page<Detection> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
