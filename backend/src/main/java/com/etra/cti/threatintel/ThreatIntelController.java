package com.etra.cti.threatintel;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.threatintel.dto.CreateIocRequest;
import com.etra.cti.threatintel.dto.IocDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/threat-intel/iocs")
public class ThreatIntelController {

    private final ThreatIntelService service;

    public ThreatIntelController(ThreatIntelService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public IocDto add(@Valid @RequestBody CreateIocRequest req,
                      @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.add(req, me.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<IocDto> list(@RequestParam(required = false) IocType type) {
        return type == null ? service.list() : service.listByType(type);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','ANALYST')")
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
