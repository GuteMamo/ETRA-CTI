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

    public InvestigationService(InvestigationRepository investigations,
                                InvestigationFindingRepository invFindings,
                                InvestigationNoteRepository notes,
                                AttackCampaignRepository campaigns,
                                CampaignEventRepository campaignEvents,
                                FindingRepository findings) {
        this.investigations = investigations;
        this.invFindings = invFindings;
        this.notes = notes;
        this.campaigns = campaigns;
        this.campaignEvents = campaignEvents;
        this.findings = findings;
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

    public List<InvestigationDto> list(InvestigationStatus status, UUID assignedTo) {
        List<Investigation> list;
        if (status != null) {
            list = investigations.findByStatusOrderByCreatedAtDesc(status);
        } else if (assignedTo != null) {
            list = investigations.findByAssignedToOrderByCreatedAtDesc(assignedTo);
        } else {
            list = investigations.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(InvestigationDto::from).toList();
    }

    public InvestigationDto get(UUID id) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        return InvestigationDto.from(inv);
    }

    @Transactional
    public InvestigationDto updateStatus(UUID id, InvestigationStatus status) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        inv.setStatus(status);
        investigations.save(inv);
        return InvestigationDto.from(inv);
    }

    @Transactional
    public InvestigationDto assign(UUID id, UUID assignedTo) {
        Investigation inv = investigations.findById(id)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        inv.setAssignedTo(assignedTo);
        if (inv.getStatus() == InvestigationStatus.OPEN) {
            inv.setStatus(InvestigationStatus.IN_PROGRESS);
        }
        investigations.save(inv);
        return InvestigationDto.from(inv);
    }

    @Transactional
    public NoteDto addNote(UUID investigationId, String text, UUID authorId) {
        investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        InvestigationNote n = new InvestigationNote();
        n.setInvestigationId(investigationId);
        n.setAuthorId(authorId);
        n.setNote(text);
        notes.save(n);
        return NoteDto.from(n);
    }

    public List<NoteDto> listNotes(UUID investigationId) {
        return notes.findByInvestigationIdOrderByCreatedAtAsc(investigationId)
            .stream().map(NoteDto::from).toList();
    }

    @Transactional
    public void linkFinding(UUID investigationId, UUID findingId) {
        investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));
        findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        if (invFindings.existsByInvestigationIdAndFindingId(investigationId, findingId)) {
            return;
        }
        InvestigationFinding link = new InvestigationFinding();
        link.setInvestigationId(investigationId);
        link.setFindingId(findingId);
        invFindings.save(link);
    }

    public InvestigationSummaryDto summary(UUID investigationId) {
        Investigation inv = investigations.findById(investigationId)
            .orElseThrow(() -> new NotFoundException("Investigation not found"));

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

    private String nextReference() {
        long n = investigations.count() + 1;
        return String.format("INV-%05d", n);
    }
}
