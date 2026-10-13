package com.etra.cti.remediation;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.remediation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RemediationController {

    private final RemediationService service;

    public RemediationController(RemediationService service) {
        this.service = service;
    }

    // ===== WRITE ACTIONS =====

    @PostMapping("/findings/{findingId}/remediation/start")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','DEVELOPER')")
    public RemediationDto start(@PathVariable UUID findingId,
                                @Valid @RequestBody StartRemediationRequest req) {
        return service.start(findingId, req.developerId());
    }

    @PostMapping("/findings/{findingId}/remediation/ready")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','DEVELOPER')")
    public RemediationDto markFixed(@PathVariable UUID findingId,
                                    @RequestBody MarkFixedRequest req) {
        return service.markFixed(findingId, req);
    }

    @PostMapping("/findings/{findingId}/retest")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER')")
    public RetestDto retest(@PathVariable UUID findingId,
                            @Valid @RequestBody PerformRetestRequest req,
                            @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.performRetest(findingId, req, me.id());
    }

    // ===== READ ACTIONS (workflow roles only) =====
    // VIEWER is intentionally excluded ? they see finding status but not internal workflow.

    @GetMapping("/findings/{findingId}/remediation")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public RemediationDto get(@PathVariable UUID findingId) {
        return service.get(findingId);
    }

    @GetMapping("/findings/{findingId}/retests")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public List<RetestDto> listRetests(@PathVariable UUID findingId) {
        return service.listRetests(findingId);
    }

    @GetMapping("/remediations")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public List<RemediationDto> list(@RequestParam(required = false) RemediationStatus status,
                                     @RequestParam(required = false) UUID developerId) {
        return service.list(status, developerId);
    }
}
