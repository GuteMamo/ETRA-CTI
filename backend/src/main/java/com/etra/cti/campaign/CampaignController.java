package com.etra.cti.campaign;

import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampaignDto> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public CampaignDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/timeline")
    public List<CampaignEventDto> timeline(@PathVariable UUID id) {
        return service.timeline(id);
    }
}
