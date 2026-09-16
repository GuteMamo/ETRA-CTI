package com.etra.cti.application;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import com.etra.cti.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;

    @Transactional
    public ApplicationDto create(UUID projectId, CreateApplicationRequest req, UUID userId) {
        Project p = projects.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(p.getOrganizationId(), userId);

        Application a = new Application();
        a.setProjectId(projectId);
        a.setName(req.name());
        a.setType(req.type());
        a.setEnvironment(req.environment());
        a.setCriticality(req.criticality());
        a.setBaseUrl(req.baseUrl());
        a.setOwnerId(userId);
        apps.save(a);
        return ApplicationDto.from(a);
    }

    public List<ApplicationDto> list(UUID projectId, UUID userId) {
        Project p = projects.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(p.getOrganizationId(), userId);
        return apps.findByProjectId(projectId).stream().map(ApplicationDto::from).toList();
    }

    public ApplicationDto get(UUID appId, UUID userId) {
        Application a = apps.findById(appId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project p = projects.findById(a.getProjectId()).orElseThrow();
        orgService.requireMember(p.getOrganizationId(), userId);
        return ApplicationDto.from(a);
    }
}
