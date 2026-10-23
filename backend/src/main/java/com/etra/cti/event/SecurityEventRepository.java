package com.etra.cti.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    Page<SecurityEvent> findByApplicationIdOrderByEventTimeDesc(UUID applicationId, Pageable pageable);

    List<SecurityEvent> findByApplicationIdAndSourceIpAndEventTimeBetween(
        UUID applicationId, String sourceIp, Instant from, Instant to);

    List<SecurityEvent> findBySourceIpAndEventTimeAfter(String sourceIp, Instant after);

    List<SecurityEvent> findByApplicationIdAndSourceIpAndEventTimeAfter(
        UUID applicationId, String sourceIp, Instant after);

    Page<SecurityEvent> findAllByOrderByEventTimeDesc(Pageable pageable);

    Page<SecurityEvent> findByApplicationIdInOrderByEventTimeDesc(List<UUID> applicationIds, Pageable pageable);
}
