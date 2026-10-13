package com.etra.cti.organization;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.organization.dto.AddMemberRequest;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public OrganizationDto create(@Valid @RequestBody CreateOrganizationRequest req,
                                  @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(req, me.id());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<OrganizationDto> listMine(@AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listMine(me.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OrganizationDto get(@PathVariable UUID id,
                               @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me.id());
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public void addMember(@PathVariable UUID id,
                          @Valid @RequestBody AddMemberRequest req,
                          @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        service.addMember(id, req, me.id());
    }
}
