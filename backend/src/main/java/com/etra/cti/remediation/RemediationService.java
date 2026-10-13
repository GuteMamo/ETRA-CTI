package com.etra.cti.remediation;

import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.FindingStatus;
import com.etra.cti.remediation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RemediationService {

    private static final Logger log = LoggerFactory.getLogger(RemediationService.class);

    private final RemediationRepository remediations;
    private final RetestRepository retests;
    private final FindingRepository findings;
    private final AuditService audit;

    public RemediationService(RemediationRepository remediations,
                              RetestRepository retests,
                              FindingRepository findings,
                              AuditService audit) {
        this.remediations = remediations;
        this.retests = retests;
        this.findings = findings;
        this.audit = audit;
    }

    @Transactional
    public RemediationDto start(UUID findingId, UUID developerId) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));

        Remediation r = remediations.findByFindingId(findingId).orElseGet(() -> {
            Remediation nr = new Remediation();
            nr.setFindingId(findingId);
            return nr;
        });
        r.setDeveloperId(developerId);
        r.setStatus(RemediationStatus.IN_PROGRESS);
        r.setStartedAt(Instant.now());
        remediations.save(r);

        f.setStatus(FindingStatus.IN_PROGRESS);
        f.setAssignedTo(developerId);
        findings.save(f);

        audit.record(developerId, "REMEDIATION_STARTED", "Finding", findingId.toString(),
            Map.of("developerId", developerId));

        log.info(">>> Remediation started for finding {} by developer {}", findingId, developerId);
        return RemediationDto.from(r);
    }

    @Transactional
    public RemediationDto markFixed(UUID findingId, MarkFixedRequest req) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        Remediation r = remediations.findByFindingId(findingId)
            .orElseThrow(() -> new NotFoundException("Remediation not started"));

        r.setStatus(RemediationStatus.READY_FOR_RETEST);
        r.setFixNotes(req.fixNotes());
        r.setCommitRef(req.commitRef());
        r.setFixedAt(Instant.now());
        remediations.save(r);

        f.setStatus(FindingStatus.READY_FOR_RETEST);
        findings.save(f);

        audit.record(null, "REMEDIATION_READY_FOR_RETEST", "Finding", findingId.toString(),
            Map.of("commitRef", String.valueOf(req.commitRef())));

        log.info(">>> Remediation marked READY_FOR_RETEST for finding {} (commit={})",
            findingId, req.commitRef());
        return RemediationDto.from(r);
    }

    @Transactional
    public RetestDto performRetest(UUID findingId, PerformRetestRequest req, UUID performedBy) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        Remediation r = remediations.findByFindingId(findingId).orElse(null);

        Retest rt = new Retest();
        rt.setFindingId(findingId);
        rt.setRemediationId(r == null ? null : r.getId());
        rt.setPerformedBy(performedBy);
        rt.setResult(req.result());
        rt.setNotes(req.notes());
        retests.save(rt);

        switch (req.result()) {
            case PASS -> {
                f.setStatus(FindingStatus.VERIFIED);
                if (r != null) {
                    r.setStatus(RemediationStatus.VERIFIED);
                    r.setVerifiedAt(Instant.now());
                    remediations.save(r);
                }
                log.info(">>> Retest PASSED for finding {}, marking VERIFIED", findingId);
            }
            case FAIL -> {
                f.setStatus(FindingStatus.REOPENED);
                if (r != null) {
                    r.setStatus(RemediationStatus.REOPENED);
                    r.setReopenedCount(r.getReopenedCount() + 1);
                    remediations.save(r);
                }
                log.info(">>> Retest FAILED for finding {}, REOPENING (attempt #{})",
                    findingId, r == null ? 1 : r.getReopenedCount());
            }
            case INCONCLUSIVE -> log.info(">>> Retest INCONCLUSIVE for finding {}", findingId);
        }
        findings.save(f);

        audit.record(performedBy, "RETEST_PERFORMED", "Finding", findingId.toString(),
            Map.of("result", req.result().name()));

        return RetestDto.from(rt);
    }

    public RemediationDto get(UUID findingId) {
        Remediation r = remediations.findByFindingId(findingId)
            .orElseThrow(() -> new NotFoundException("No remediation for this finding"));
        return RemediationDto.from(r);
    }

    public List<RemediationDto> list(RemediationStatus status, UUID developerId) {
        List<Remediation> list;
        if (status != null) {
            list = remediations.findByStatusOrderByCreatedAtDesc(status);
        } else if (developerId != null) {
            list = remediations.findByDeveloperIdOrderByCreatedAtDesc(developerId);
        } else {
            list = remediations.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(RemediationDto::from).toList();
    }

    public List<RetestDto> listRetests(UUID findingId) {
        return retests.findByFindingIdOrderByCreatedAtDesc(findingId)
            .stream().map(RetestDto::from).toList();
    }
}
