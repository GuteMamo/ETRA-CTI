package com.etra.cti.evidence;

import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.evidence.dto.EvidenceDto;
import com.etra.cti.evidence.dto.EvidenceNoteDto;
import com.etra.cti.evidence.dto.IntegrityResultDto;
import com.etra.cti.finding.Finding;
import com.etra.cti.finding.FindingRepository;
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
    private final Path storageRoot;

    public EvidenceService(EvidenceRepository evidenceRepo,
                           EvidenceNoteRepository noteRepo,
                           FindingRepository findings,
                           @Value("${etra.evidence.storage-path}") String storagePath) throws IOException {
        this.evidenceRepo = evidenceRepo;
        this.noteRepo = noteRepo;
        this.findings = findings;
        this.storageRoot = Paths.get(storagePath);
        Files.createDirectories(storageRoot);
        log.info(">>> EvidenceService initialized, storage root: {}", storageRoot.toAbsolutePath());
    }

    @Transactional
    public EvidenceDto upload(UUID findingId, MultipartFile file, UUID userId) throws IOException {
        Finding f = findings.findById(findingId)
            .orElseThrow(() -> new NotFoundException("Finding not found"));

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

    public List<EvidenceDto> listForFinding(UUID findingId) {
        return evidenceRepo.findByFindingId(findingId).stream().map(EvidenceDto::from).toList();
    }

    public EvidenceDto get(UUID id) {
        Evidence e = evidenceRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));
        return EvidenceDto.from(e);
    }

    public IntegrityResultDto verifyIntegrity(UUID evidenceId) throws IOException {
        Evidence e = evidenceRepo.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));

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

    @Transactional
    public EvidenceNoteDto addNote(UUID evidenceId, String note, UUID authorId) {
        evidenceRepo.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("Evidence not found"));

        EvidenceNote n = new EvidenceNote();
        n.setEvidenceId(evidenceId);
        n.setAuthorId(authorId);
        n.setNote(note);
        noteRepo.save(n);
        return EvidenceNoteDto.from(n);
    }

    public List<EvidenceNoteDto> listNotes(UUID evidenceId) {
        return noteRepo.findByEvidenceIdOrderByCreatedAtDesc(evidenceId)
            .stream().map(EvidenceNoteDto::from).toList();
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
