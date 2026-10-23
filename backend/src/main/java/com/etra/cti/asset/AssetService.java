package com.etra.cti.asset;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.asset.dto.AssetDto;
import com.etra.cti.asset.dto.CreateAssetRequest;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.organization.dto.OrganizationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assets;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;

    public AssetService(AssetRepository assets, ApplicationRepository apps, ProjectRepository projects, OrganizationService orgService) {
        this.assets = assets;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
    }

    @Transactional
    public AssetDto create(UUID appId, CreateAssetRequest req, UUID userId) {
        Application app = apps.findById(appId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project p = projects.findById(app.getProjectId()).orElseThrow();
        orgService.requireMember(p.getOrganizationId(), userId);

        Asset a = new Asset();
        a.setApplicationId(appId);
        a.setType(req.type());
        a.setHostname(req.hostname());
        a.setUrl(req.url());
        a.setPort(req.port());
        a.setProtocol(req.protocol());
        a.setEnvironment(req.environment());
        a.setCriticality(req.criticality());
        a.setStatus("ACTIVE");
        assets.save(a);
        return AssetDto.from(a);
    }

    public List<AssetDto> list(UUID appId, UUID userId) {
        Application app = apps.findById(appId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project p = projects.findById(app.getProjectId()).orElseThrow();
        orgService.requireMember(p.getOrganizationId(), userId);
        return assets.findByApplicationId(appId).stream().map(AssetDto::from).toList();
    }

    public List<AssetDto> listAll(JwtAuthFilter.AuthedUser caller) {
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role()) && !orgService.isPlatformStaff(caller.id())) {
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
            return assets.findByApplicationIdIn(allowedAppIds).stream().map(AssetDto::from).toList();
        }
        return assets.findAll().stream().map(AssetDto::from).toList();
    }

    public List<AssetDto> listAll(UUID userId) {
        return assets.findAll().stream().map(AssetDto::from).toList();
    }

    public AssetDto get(UUID assetId, JwtAuthFilter.AuthedUser caller) {
        Asset a = assets.findById(assetId)
            .orElseThrow(() -> new NotFoundException("Asset not found"));
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            Application app = apps.findById(a.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
            Project p = projects.findById(app.getProjectId()).orElseThrow();
            orgService.requireMember(p.getOrganizationId(), caller.id());
        }
        return AssetDto.from(a);
    }

    public AssetDto get(UUID assetId, UUID userId) {
        Asset a = assets.findById(assetId)
            .orElseThrow(() -> new NotFoundException("Asset not found"));
        if (userId != null) {
            Application app = apps.findById(a.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
            Project p = projects.findById(app.getProjectId()).orElseThrow();
            orgService.requireMember(p.getOrganizationId(), userId);
        }
        return AssetDto.from(a);
    }
}
