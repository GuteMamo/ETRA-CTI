package com.etra.cti.detection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionRepository extends JpaRepository<Detection, UUID> {
    List<Detection> findByEventId(UUID eventId);
}
