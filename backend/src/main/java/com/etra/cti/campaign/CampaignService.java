package com.etra.cti.campaign;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;

    public CampaignService(AttackCampaignRepository campaigns,
                           CampaignEventRepository campaignEvents,
                           ApplicationRepository apps,
                           ProjectRepository projects,
                           OrganizationService orgService) {
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
    }

    public List<CampaignDto> listAll(JwtAuthFilter.AuthedUser caller) {
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            List<UUID> orgIds = orgService.listMine(caller.id()).stream()
                .map(OrganizationDto::id)
                .toList();
            if (orgIds.isEmpty()) {
                return List.of();
            }
            List<UUID> allowedAppIds = orgIds.stream()
                .flatMap(orgId -> projects.findByOrganizationId(orgId).stream())
                .flatMap(project -> apps.findByProjectId(project.getId()).stream())
                .map(Application::getId)
                .distinct()
                .toList();
            if (allowedAppIds.isEmpty()) {
                return List.of();
            }
            return campaigns.findByApplicationIdInOrderByLastSeenDesc(allowedAppIds).stream().map(CampaignDto::from).toList();
        }
        return campaigns.findAllByOrderByLastSeenDesc().stream().map(CampaignDto::from).toList();
    }

    public List<CampaignDto> listAll() {
        return listAll(null);
    }

    public CampaignDto get(UUID id, JwtAuthFilter.AuthedUser caller) {
        AttackCampaign c = campaigns.findById(id)
            .orElseThrow(() -> new NotFoundException("Campaign not found"));
        validateAccess(c, caller);
        return CampaignDto.from(c);
    }

    public CampaignDto get(UUID id) {
        return get(id, null);
    }

    public List<CampaignEventDto> timeline(UUID campaignId, JwtAuthFilter.AuthedUser caller) {
        AttackCampaign c = campaigns.findById(campaignId)
            .orElseThrow(() -> new NotFoundException("Campaign not found"));
        validateAccess(c, caller);
        return campaignEvents.findByCampaignIdOrderBySequenceNoAsc(campaignId)
            .stream().map(CampaignEventDto::from).toList();
    }

    public List<CampaignEventDto> timeline(UUID campaignId) {
        return timeline(campaignId, null);
    }

    private void validateAccess(AttackCampaign c, JwtAuthFilter.AuthedUser caller) {
        if (caller == null || "ADMIN".equalsIgnoreCase(caller.role())) {
            return;
        }
        if (c.getApplicationId() == null) {
            return;
        }
        Application app = apps.findById(c.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projects.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), caller.id());
    }
}
