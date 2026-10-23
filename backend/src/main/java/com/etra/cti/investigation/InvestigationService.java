package com.etra.cti.investigation;

import com.etra.cti.campaign.AttackCampaign;
import com.etra.cti.campaign.AttackCampaignRepository;
import com.etra.cti.campaign.CampaignEventRepository;
import com.etra.cti.campaign.dto.CampaignDto;
import com.etra.cti.campaign.dto.CampaignEventDto;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.dto.FindingDto;
import com.etra.cti.investigation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InvestigationService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

    private final InvestigationRepository investigations;
    private final InvestigationFindingRepository invFindings;
    private final InvestigationNoteRepository notes;
    private final AttackCampaignRepository campaigns;
    private final CampaignEventRepository campaignEvents;
    private final FindingRepository findings;
    private final com.etra.cti.application.ApplicationRepository apps;
    private final com.etra.cti.project.ProjectRepository projects;
    private final com.etra.cti.organization.OrganizationService orgService;

    public InvestigationService(InvestigationRepository investigations,
                                InvestigationFindingRepository invFindings,
                                InvestigationNoteRepository notes,
                                AttackCampaignRepository campaigns,
                                CampaignEventRepository campaignEvents,
                                FindingRepository findings,
                                com.etra.cti.application.ApplicationRepository apps,
                                com.etra.cti.project.ProjectRepository projects,
                                com.etra.cti.organization.OrganizationService orgService) {
        this.investigations = investigations;
        this.invFindings = invFindings;
        this.notes = notes;
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.findings = findings;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
    }

    @Transactional
    public InvestigationDto create(CreateInvestigationRequest req, com.etra.cti.auth.JwtAuthFilter.AuthedUser me) {
        if (me != null && !"ADMIN".equalsIgnoreCase(me.role())) {
            com.etra.cti.application.Application app = apps.findById(req.applicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
            com.etra.cti.project.Project project = projects.findById(app.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
            orgService.requireMember(project.getOrganizationId(), me.id());
        }

        Investigation inv = new Investigation();
        inv.setReference(nextReference());
        inv.setCampaignId(req.campaignId());
        inv.setApplicationId(req.applicationId());
        inv.setTitle(req.title());
        inv.setDescription(req.description());
        inv.setPriority(req.priority() == null ? InvestigationPriority.MEDIUM : req.priority());
        inv.setStatus(InvestigationStatus.OPEN);
        inv.setCreatedBy(me != null ? me.id() : null);
        investigations.save(inv);
        log.info(">>> Created investigation {} for campaign {}", inv.getReference(), req.campaignId());
        return InvestigationDto.from(inv);
    }

    @Transactional
    public InvestigationDto create(CreateInvestigationRequest req, UUID createdBy) {
        Investigation inv = new Investigation();
        inv.setReference(nextReference());
        inv.setCampaignId(req.campaignId());
        inv.setApplicationId(req.applicationId());
        inv.setTitle(req.title());
        inv.setDescription(req.description());
        inv.setPriority(req.priority() == null ? InvestigationPriority.MEDIUM : req.priority());
        inv.setStatus(InvestigationStatus.OPEN);
        inv.setCreatedBy(createdBy);
        investigations.save(inv);
        log.info(">>> Created investigation {} for campaign {}", inv.getReference(), req.campaignId());
        return InvestigationDto.from(inv);
    }

    public List<InvestigationDto> list(InvestigationStatus status, UUID assignedTo, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        List<UUID> allowedAppIds = null;
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role()) && !orgService.isPlatformStaff(caller.id())) {
            List<UUID> orgIds = orgService.listMine(caller.id()).stream()
                .map(com.etra.cti.organization.dto.OrganizationDto::id)
                .toList();
            if (orgIds.isEmpty()) {
                return List.of();
            }
            allowedAppIds = orgIds.stream()
                .flatMap(orgId -> projects.findByOrganizationId(orgId).stream())
                .flatMap(project -> apps.findByProjectId(project.getId()).stream())
                .map(com.etra.cti.application.Application::getId)
                .distinct()
                .toList();
            if (allowedAppIds.isEmpty()) {
                return List.of();
            }
        }

        final List<UUID> scopedAppIds = allowedAppIds;
        org.springframework.data.jpa.domain.Specification<Investigation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (scopedAppIds != null) {
                predicates.add(root.get("applicationId").in(scopedAppIds));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo"), assignedTo));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return investigations.findAll(spec).stream().map(InvestigationDto::from).toList();
    }

    public List<InvestigationDto> list(InvestigationStatus status, UUID assignedTo) {
        return list(status, assignedTo, null);
    }

    public InvestigationDto get(UUID id, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        return InvestigationDto.from(inv);
    }

    public InvestigationDto get(UUID id) {
        return get(id, null);
    }

    @Transactional
    public InvestigationDto updateStatus(UUID id, InvestigationStatus status, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        inv.setStatus(status);
        investigations.save(inv);
        return InvestigationDto.from(inv);
    }

    @Transactional
    public InvestigationDto updateStatus(UUID id, InvestigationStatus status) {
        return updateStatus(id, status, null);
    }

    @Transactional
    public InvestigationDto assign(UUID id, UUID assignedTo, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        inv.setAssignedTo(assignedTo);
        if (inv.getStatus() == InvestigationStatus.OPEN) {
            inv.setStatus(InvestigationStatus.IN_PROGRESS);
        }
        investigations.save(inv);
        return InvestigationDto.from(inv);
    }

    @Transactional
    public InvestigationDto assign(UUID id, UUID assignedTo) {
        return assign(id, assignedTo, null);
    }

    @Transactional
    public NoteDto addNote(UUID investigationId, String text, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        InvestigationNote n = new InvestigationNote();
        n.setInvestigationId(investigationId);
        n.setAuthorId(caller != null ? caller.id() : null);
        n.setNote(text);
        notes.save(n);
        return NoteDto.from(n);
    }

    @Transactional
    public NoteDto addNote(UUID investigationId, String text, UUID authorId) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        InvestigationNote n = new InvestigationNote();
        n.setInvestigationId(investigationId);
        n.setAuthorId(authorId);
        n.setNote(text);
        notes.save(n);
        return NoteDto.from(n);
    }

    public List<NoteDto> listNotes(UUID investigationId, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        return notes.findByInvestigationIdOrderByCreatedAtAsc(investigationId)
            .stream().map(NoteDto::from).toList();
    }

    public List<NoteDto> listNotes(UUID investigationId) {
        return listNotes(investigationId, null);
    }

    @Transactional
    public void linkFinding(UUID investigationId, UUID findingId, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        if (caller != null && !"ADMIN".equalsIgnoreCase(caller.role())) {
            com.etra.cti.application.Application app = apps.findById(f.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
            com.etra.cti.project.Project project = projects.findById(app.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
            orgService.requireMember(project.getOrganizationId(), caller.id());
        }
        if (invFindings.existsByInvestigationIdAndFindingId(investigationId, findingId)) {
            return;
        }
        InvestigationFinding link = new InvestigationFinding();
        link.setInvestigationId(investigationId);
        link.setFindingId(findingId);
        invFindings.save(link);
    }

    @Transactional
    public void linkFinding(UUID investigationId, UUID findingId) {
        linkFinding(investigationId, findingId, null);
    }

    public InvestigationSummaryDto summary(UUID investigationId, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        validateAccess(inv, caller);

        CampaignDto campaign = null;
        List<CampaignEventDto> timeline = List.of();

        if (inv.getCampaignId() != null) {
            AttackCampaign c = campaigns.findById(inv.getCampaignId()).orElse(null);
            if (c != null) {
                campaign = CampaignDto.from(c);
                timeline = campaignEvents.findByCampaignIdOrderBySequenceNoAsc(c.getId())
                    .stream().map(CampaignEventDto::from).toList();
            }
        }

        List<FindingDto> findingDtos = invFindings.findByInvestigationId(investigationId).stream()
            .map(link -> findings.findById(link.getFindingId()).orElse(null))
            .filter(f -> f != null)
            .map(FindingDto::from)
            .toList();

        List<NoteDto> noteDtos = notes.findByInvestigationIdOrderByCreatedAtAsc(investigationId)
            .stream().map(NoteDto::from).toList();

        return new InvestigationSummaryDto(
            InvestigationDto.from(inv), campaign, timeline, findingDtos, noteDtos);
    }

    public InvestigationSummaryDto summary(UUID investigationId) {
        return summary(investigationId, null);
    }

    private void validateAccess(Investigation inv, com.etra.cti.auth.JwtAuthFilter.AuthedUser caller) {
        if (caller == null || "ADMIN".equalsIgnoreCase(caller.role())) {
            return;
        }
        if (inv.getApplicationId() == null) {
            return;
        }
        com.etra.cti.application.Application app = apps.findById(inv.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));
        com.etra.cti.project.Project project = projects.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), caller.id());
    }

    private String nextReference() {
        long n = investigations.count() + 1;
        return String.format("INV-%05d", n);
    }
}
