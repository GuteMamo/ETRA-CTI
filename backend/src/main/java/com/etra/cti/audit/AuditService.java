package com.etra.cti.audit;

import com.etra.cti.audit.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(UUID userId,
                       String action,
                       String resourceType,
                       String resourceId,
                       Map<String, Object> newValue) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setNewValue(newValue);
        repo.save(log);
    }

    @Transactional
    public void recordChange(UUID userId,
                             String action,
                             String resourceType,
                             String resourceId,
                             Map<String, Object> oldValue,
                             Map<String, Object> newValue) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        repo.save(log);
    }

    public Page<AuditLogDto> query(String resourceType,
                                   String resourceId,
                                   UUID userId,
                                   Instant from,
                                   Instant to,
                                   Pageable pageable) {
        Page<AuditLog> page;
        if (resourceType != null && resourceId != null) {
            page = repo.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId, pageable);
        } else if (userId != null) {
            page = repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (from != null && to != null) {
            page = repo.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable);
        } else {
            page = repo.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(AuditLogDto::from);
    }

    public List<AuditLogDto> trail(String resourceType, String resourceId) {
        return repo.findByResourceTypeAndResourceIdOrderByCreatedAtAsc(resourceType, resourceId)
            .stream().map(AuditLogDto::from).toList();
    }
}
