package com.etra.cti.application;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.auth.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping("/projects/{projectId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("!hasRole('VIEWER')")
    public ApplicationDto create(@PathVariable UUID projectId,
                                 @Valid @RequestBody CreateApplicationRequest req,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(projectId, req, me.id());
    }

    @GetMapping("/projects/{projectId}/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ApplicationDto> list(@PathVariable UUID projectId,
                                     @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.list(projectId, me.id());
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ApplicationDto> listAll(@AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listAll(me.id());
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public ApplicationDto get(@PathVariable UUID id,
                              @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me.id());
    }
}
