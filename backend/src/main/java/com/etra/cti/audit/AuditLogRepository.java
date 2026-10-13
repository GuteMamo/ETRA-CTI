package com.etra.cti.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
        String resourceType, String resourceId, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        Instant from, Instant to, Pageable pageable);

    List<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtAsc(
        String resourceType, String resourceId);
}
