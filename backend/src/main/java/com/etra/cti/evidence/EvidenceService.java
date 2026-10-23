package com.etra.cti.evidence;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.evidence.dto.EvidenceDto;
import com.etra.cti.evidence.dto.EvidenceNoteDto;
import com.etra.cti.evidence.dto.IntegrityResultDto;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    private final EvidenceRepository evidenceRepo;
    private final EvidenceNoteRepository noteRepo;
    private final FindingRepository findings;
    private final ApplicationRepository apps;
    private final ProjectRepository projects;
    private final OrganizationService orgService;
    private final Path storageRoot;

    public EvidenceService(EvidenceRepository evidenceRepo,
                           EvidenceNoteRepository noteRepo,
                           FindingRepository findings,
                           ApplicationRepository apps,
                           ProjectRepository projects,
                           OrganizationService orgService,
                           @Value("${etra.evidence.storage-path}") String storagePath) throws IOException {
        this.evidenceRepo = evidenceRepo;
        this.noteRepo = noteRepo;
        this.findings = findings;
        this.apps = apps;
        this.projects = projects;
        this.orgService = orgService;
        this.storageRoot = Paths.get(storagePath);
        Files.createDirectories(storageRoot);
        log.info(">>> EvidenceService initialized, storage root: {}", storageRoot.toAbsolutePath());
    }

    @Transactional
    public EvidenceDto upload(UUID findingId, MultipartFile file, UUID userId) throws IOException {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        validateAccess(f, userId);

        Path findingDir = storageRoot.resolve(findingId.toString());
        Files.createDirectories(findingDir);

        String original = sanitize(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + original;
        Path destination = findingDir.resolve(storedName);

        // Save the file
        file.transferTo(destination);

        // Compute SHA-256 while reading
        String sha = sha256(destination);

        Evidence e = new Evidence();
        e.setFindingId(f.getId());
        e.setFilename(original);
        e.setContentType(file.getContentType());
        e.setSizeBytes(Files.size(destination));
        e.setStoragePath(destination.toString());
        e.setSha256(sha);
        e.setCreatedBy(userId);

        evidenceRepo.save(e);
        log.info(">>> Evidence {} uploaded for finding {} (sha256={}...)", e.getId(), findingId, sha.substring(0, 12));
        return EvidenceDto.from(e);
    }

    public List<EvidenceDto> listForFinding(UUID findingId, UUID userId) {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        validateAccess(f, userId);
        return evidenceRepo.findByFindingId(findingId).stream().map(EvidenceDto::from).toList();
    }

    public List<EvidenceDto> listForFinding(UUID findingId) {
        return listForFinding(findingId, null);
    }

    public EvidenceDto get(UUID id, UUID userId) {
        Evidence e = evidenceRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));
        validateAccess(e, userId);
        return EvidenceDto.from(e);
    }

    public EvidenceDto get(UUID id) {
        return get(id, null);
    }

    public IntegrityResultDto verifyIntegrity(UUID evidenceId, UUID userId) throws IOException {
        Evidence e = evidenceRepo.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));
        validateAccess(e, userId);

        Path p = Paths.get(e.getStoragePath());
        if (!Files.exists(p)) {
            return new IntegrityResultDto(e.getId(), e.getFilename(),
                "FILE_MISSING", e.getSha256(), null, 0L);
        }

        String current = sha256(p);
        String status = current.equalsIgnoreCase(e.getSha256()) ? "INTACT" : "TAMPERED";

        return new IntegrityResultDto(e.getId(), e.getFilename(),
            status, e.getSha256(), current, Files.size(p));
    }

    public IntegrityResultDto verifyIntegrity(UUID evidenceId) throws IOException {
        return verifyIntegrity(evidenceId, null);
    }

    @Transactional
    public EvidenceNoteDto addNote(UUID evidenceId, String note, UUID authorId) {
        Evidence e = evidenceRepo.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));
        validateAccess(e, authorId);

        EvidenceNote n = new EvidenceNote();
        n.setEvidenceId(evidenceId);
        n.setAuthorId(authorId);
        n.setNote(note);
        noteRepo.save(n);
        return EvidenceNoteDto.from(n);
    }

    public List<EvidenceNoteDto> listNotes(UUID evidenceId, UUID userId) {
        Evidence e = evidenceRepo.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));
        validateAccess(e, userId);
        return noteRepo.findByEvidenceIdOrderByCreatedAtDesc(evidenceId)
            .stream().map(EvidenceNoteDto::from).toList();
    }

    public List<EvidenceNoteDto> listNotes(UUID evidenceId) {
        return listNotes(evidenceId, null);
    }

    private void validateAccess(Finding f, UUID userId) {
        if (userId == null) return;
        Application app = apps.findById(f.getApplicationId())
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projects.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), userId);
    }

    private void validateAccess(Evidence e, UUID userId) {
        if (userId == null) return;
        Finding f = findings.findById(e.getFindingId())
            .orElseThrow(() -> new NotFoundException("Finding not found"));
        validateAccess(f, userId);
    }

    // ---------- helpers ----------

    private String sha256(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "evidence.bin";
        // Strip path separators and dangerous chars
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
