package com.etra.cti.asset;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.asset.dto.AssetDto;
import com.etra.cti.asset.dto.CreateAssetRequest;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assets;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;

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
}
