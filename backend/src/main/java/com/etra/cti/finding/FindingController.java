package com.etra.cti.finding;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.detection.Severity;
import com.etra.cti.finding.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class FindingController {

    private final FindingService service;

    public FindingController(FindingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public FindingDto create(@Valid @RequestBody CreateFindingRequest req,
                             @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(req, me.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public Page<FindingDto> list(
        @RequestParam(required = false) UUID applicationId,
        @RequestParam(required = false) FindingStatus status,
        @RequestParam(required = false) Severity severity,
        @RequestParam(required = false) UUID assignedTo,
        @RequestParam(required = false) Boolean overdue,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (applicationId == null && status == null && severity == null && assignedTo == null && overdue == null) {
            return service.list(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "riskScore")));
        }
        return service.list(applicationId, status, severity, assignedTo, overdue, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "riskScore")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public FindingDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','DEVELOPER')")
    public FindingDto updateStatus(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateStatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    @RequestMapping(value = "/{id}/assign", method = {RequestMethod.POST, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public FindingDto assign(@PathVariable UUID id,
                             @Valid @RequestBody AssignFindingRequest req,
                             @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.assign(id, req.assignedTo(), req.dueDate(), me != null ? me.id() : null);
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public FindingDto overridePriority(@PathVariable UUID id,
                                       @Valid @RequestBody OverridePriorityRequest req,
                                       @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.overridePriority(id, req, me != null ? me.id() : null);
    }

    @PatchMapping("/{id}/due-date")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public FindingDto setDueDate(@PathVariable UUID id,
                                 @RequestBody Map<String, String> body,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        String dueStr = body.get("dueDate");
        Instant dueDate = (dueStr != null && !dueStr.isBlank()) ? Instant.parse(dueStr) : null;
        return service.setDueDate(id, dueDate, me != null ? me.id() : null);
    }
}
