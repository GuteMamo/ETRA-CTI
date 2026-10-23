package com.etra.cti.alert;

import com.etra.cti.auth.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public Page<AlertDto> list(
        @RequestParam(required = false) AlertStatus status,
        @RequestParam(required = false) UUID applicationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @AuthenticationPrincipal JwtAuthFilter.AuthedUser me
    ) {
        return alertService.list(status, applicationId, PageRequest.of(page, size), me);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public AlertDto get(@PathVariable UUID id,
                        @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return alertService.get(id, me);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public AlertDto acknowledge(@PathVariable UUID id,
                                @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return alertService.acknowledge(id, me);
    }

    @PostMapping("/{id}/triage")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public AlertDto triage(@PathVariable UUID id,
                           @Valid @RequestBody TriageAlertRequest req,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return alertService.triage(id, req, me);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public AlertDto updateStatus(@PathVariable UUID id,
                                 @Valid @RequestBody TriageAlertRequest req,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return alertService.triage(id, req, me);
    }
}
