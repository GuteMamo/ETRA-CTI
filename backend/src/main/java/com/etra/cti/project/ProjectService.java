package com.etra.cti.project;

import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projects;
    private final OrganizationService orgService;

    @Transactional
    public ProjectDto create(UUID orgId, CreateProjectRequest req, UUID userId) {
        orgService.requireMember(orgId, userId);

        Project p = new Project();
        p.setOrganizationId(orgId);
        p.setName(req.name());
        p.setDescription(req.description());
        projects.save(p);
        return ProjectDto.from(p);
    }

    public List<ProjectDto> list(UUID orgId, UUID userId) {
        orgService.requireMember(orgId, userId);
        return projects.findByOrganizationId(orgId).stream().map(ProjectDto::from).toList();
    }

    public ProjectDto get(UUID projectId, UUID userId) {
        Project p = projects.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(p.getOrganizationId(), userId);
        return ProjectDto.from(p);
    }
}
