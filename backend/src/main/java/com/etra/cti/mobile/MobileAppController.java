package com.etra.cti.mobile;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.finding.Finding;
import com.etra.cti.mobile.dto.MobileAppDto;
import com.etra.cti.mobile.dto.RegisterMobileAppRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mobile-apps")
public class MobileAppController {

    private final MobileAppService service;

    public MobileAppController(MobileAppService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER')")
    public MobileAppDto register(@Valid @RequestBody RegisterMobileAppRequest req,
                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.register(req, me.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<MobileAppDto> list() {
        return service.list();
    }

    @GetMapping("/by-application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<MobileAppDto> listByApplication(@PathVariable UUID applicationId) {
        return service.listByApplication(applicationId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public MobileAppDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping(value = "/{id}/import-mobsf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public Map<String, Object> importMobSF(@PathVariable UUID id,
                                           @RequestPart("file") MultipartFile file,
                                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.importMobSF(id, file, me.id());
    }

    @GetMapping("/{id}/findings")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<Finding> findings(@PathVariable UUID id) {
        return service.findingsFor(id);
    }
}
