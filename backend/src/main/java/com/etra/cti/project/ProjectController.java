package com.etra.cti.project;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping("/organizations/{orgId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("!hasRole('VIEWER')")
    public ProjectDto create(@PathVariable UUID orgId,
                             @Valid @RequestBody CreateProjectRequest req,
                             @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(orgId, req, me.id());
    }

    @GetMapping("/organizations/{orgId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ProjectDto> list(@PathVariable UUID orgId,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.list(orgId, me.id());
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ProjectDto> listAll(@AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listAll(me.id());
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public ProjectDto get(@PathVariable UUID id,
                          @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me.id());
    }
}
