package com.etra.cti.integration;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.integration.dto.ImportDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService service;

    public IntegrationController(IntegrationService service) {
        this.service = service;
    }

    @PostMapping(value = "/burp/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public ImportDto importBurp(@PathVariable UUID applicationId,
                                @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.importBurp(applicationId, file, me.id());
    }

    @PostMapping(value = "/zap/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public ImportDto importZap(@PathVariable UUID applicationId,
                               @RequestPart("file") MultipartFile file,
                               @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.importZap(applicationId, file, me.id());
    }

    @PostMapping(value = "/nmap/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public ImportDto importNmap(@PathVariable UUID applicationId,
                                @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.importNmap(applicationId, file, me.id());
    }

    @GetMapping("/imports")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ImportDto> list() {
        return service.listAll();
    }

    @GetMapping("/imports/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public ImportDto get(@PathVariable UUID id) {
        return service.get(id);
    }
}
