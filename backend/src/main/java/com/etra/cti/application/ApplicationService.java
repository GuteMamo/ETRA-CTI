package com.etra.cti.application;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import com.etra.cti.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;
    private final UserRepository users;

    public ApplicationService(ApplicationRepository apps, ProjectRepository projects, OrganizationService orgService, UserRepository users) {
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
        this.users = users;
    }

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

    @Transactional
    public ApplicationDto create(CreateApplicationRequest req, UUID projectId, UUID userId) {
        UUID targetProjectId = projectId != null ? projectId : req.projectId();
        if (targetProjectId == null) {
            List<UUID> orgIds = orgService.listMine(userId).stream()
                .map(OrganizationDto::id)
                .toList();
            List<Project> userProjects = orgIds.stream()
                .flatMap(orgId -> projects.findByOrganizationId(orgId).stream())
                .toList();
            if (userProjects.isEmpty()) {
                if (!orgIds.isEmpty()) {
                    UUID orgId = orgIds.get(0);
                    Project defaultProject = new Project();
                    defaultProject.setOrganizationId(orgId);
                    defaultProject.setName("Default Project");
                    defaultProject.setDescription("Auto-created default project for applications");
                    projects.save(defaultProject);
                    targetProjectId = defaultProject.getId();
                } else {
                    User u = users.findById(userId).orElse(null);
                    if (u != null && u.getRole() == UserRole.ADMIN) {
                        List<Project> allProjects = projects.findAll();
                        if (allProjects.isEmpty()) {
                            throw new NotFoundException("No project found to register application under");
                        }
                        targetProjectId = allProjects.get(0).getId();
                    } else {
                        throw new org.springframework.security.access.AccessDeniedException("User does not belong to any organization");
                    }
                }
            } else {
                targetProjectId = userProjects.get(0).getId();
            }
        }
        return create(targetProjectId, req, userId);
    }

    public List<ApplicationDto> list(UUID projectId, UUID userId) {
        Project p = projects.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(p.getOrganizationId(), userId);
        return apps.findByProjectId(projectId).stream().map(ApplicationDto::from).toList();
    }

    public List<ApplicationDto> listAll(UUID userId) {
        User u = users.findById(userId).orElse(null);
        if (u != null && orgService.isPlatformStaff(u)) {
            return apps.findAll().stream().map(ApplicationDto::from).toList();
        }
        List<UUID> orgIds = orgService.listMine(userId).stream()
            .map(OrganizationDto::id)
            .toList();
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return orgIds.stream()
            .flatMap(orgId -> projects.findByOrganizationId(orgId).stream())
            .flatMap(project -> apps.findByProjectId(project.getId()).stream())
            .distinct()
            .map(ApplicationDto::from)
            .toList();
    }

    public ApplicationDto get(UUID appId, UUID userId) {
        Application a = apps.findById(appId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project p = projects.findById(a.getProjectId()).orElseThrow();
        orgService.requireMember(p.getOrganizationId(), userId);
        return ApplicationDto.from(a);
    }
}
