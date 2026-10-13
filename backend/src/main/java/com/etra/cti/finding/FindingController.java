package com.etra.cti.finding;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.finding.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public Page<FindingDto> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public FindingDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public FindingDto updateStatus(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateStatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public FindingDto assign(@PathVariable UUID id,
                             @Valid @RequestBody AssignFindingRequest req) {
        return service.assign(id, req.assignedTo());
    }
}
