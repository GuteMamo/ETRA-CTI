package com.etra.cti.campaign;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampaignDto> list(@AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listAll(me);
    }

    @GetMapping("/{id}")
    public CampaignDto get(@PathVariable UUID id,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me);
    }

    @GetMapping("/{id}/timeline")
    public List<CampaignEventDto> timeline(@PathVariable UUID id,
                                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.timeline(id, me);
    }
}
