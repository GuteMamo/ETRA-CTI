package com.etra.cti.application;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.auth.JwtAuthFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @PostMapping("/projects/{projectId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@PathVariable UUID projectId,
                                 @Valid @RequestBody CreateApplicationRequest req,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(projectId, req, me.id());
    }

    @GetMapping("/projects/{projectId}/applications")
    public List<ApplicationDto> list(@PathVariable UUID projectId,
                                     @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.list(projectId, me.id());
    }

    @GetMapping("/applications/{id}")
    public ApplicationDto get(@PathVariable UUID id,
                              @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me.id());
    }
}
