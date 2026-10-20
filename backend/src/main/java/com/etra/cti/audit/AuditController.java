package com.etra.cti.audit;

import com.etra.cti.audit.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/audit", "/api/audit-logs"})
@PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','VIEWER')")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditLogDto> query(
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String resourceId,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return service.query(resourceType, resourceId, userId, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/trail/{resourceType}/{resourceId}")
    public List<AuditLogDto> trail(@PathVariable String resourceType,
                                   @PathVariable String resourceId) {
        return service.trail(resourceType, resourceId);
    }
}
