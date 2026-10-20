package com.etra.cti.investigation;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.investigation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationService service;

    public InvestigationController(InvestigationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public InvestigationDto create(@Valid @RequestBody CreateInvestigationRequest req,
                                   @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(req, me.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','PENTESTER','DEVELOPER','VIEWER')")
    public List<InvestigationDto> list(@RequestParam(required = false) InvestigationStatus status,
                                       @RequestParam(required = false) UUID assignedTo) {
        return service.list(status, assignedTo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','PENTESTER','DEVELOPER','VIEWER')")
    public InvestigationDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','PENTESTER','DEVELOPER','VIEWER')")
    public InvestigationSummaryDto summary(@PathVariable UUID id) {
        return service.summary(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public InvestigationDto updateStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateInvestigationStatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public InvestigationDto assign(@PathVariable UUID id,
                                   @Valid @RequestBody AssignInvestigationRequest req) {
        return service.assign(id, req.assignedTo());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public NoteDto addNote(@PathVariable UUID id,
                           @Valid @RequestBody AddNoteRequest req,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.addNote(id, req.note(), me.id());
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST','PENTESTER','DEVELOPER','VIEWER')")
    public List<NoteDto> notes(@PathVariable UUID id) {
        return service.listNotes(id);
    }

    @PostMapping("/{id}/findings/{findingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public void linkFinding(@PathVariable UUID id, @PathVariable UUID findingId) {
        service.linkFinding(id, findingId);
    }
}
