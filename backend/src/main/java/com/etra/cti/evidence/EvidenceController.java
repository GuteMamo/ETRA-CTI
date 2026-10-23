package com.etra.cti.evidence;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.evidence.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EvidenceController {

    private final EvidenceService service;

    public EvidenceController(EvidenceService service) {
        this.service = service;
    }

    @PostMapping(value = "/findings/{findingId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public EvidenceDto upload(@PathVariable UUID findingId,
                              @RequestPart("file") MultipartFile file,
                              @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) throws IOException {
        return service.upload(findingId, file, me.id());
    }

    @PostMapping(value = "/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public EvidenceDto uploadRoot(@RequestParam("findingId") UUID findingId,
                                  @RequestPart("file") MultipartFile file,
                                  @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) throws IOException {
        return service.upload(findingId, file, me.id());
    }

    @GetMapping("/findings/{findingId}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public List<EvidenceDto> listForFinding(@PathVariable UUID findingId,
                                            @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listForFinding(findingId, me != null ? me.id() : null);
    }

    @GetMapping("/evidence/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public EvidenceDto get(@PathVariable UUID id,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.get(id, me != null ? me.id() : null);
    }

    @GetMapping("/evidence/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public IntegrityResultDto verifyGet(@PathVariable UUID id,
                                        @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) throws IOException {
        return service.verifyIntegrity(id, me != null ? me.id() : null);
    }

    @PostMapping("/evidence/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public IntegrityResultDto verifyPost(@PathVariable UUID id,
                                         @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) throws IOException {
        return service.verifyIntegrity(id, me != null ? me.id() : null);
    }

    @PostMapping("/evidence/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST')")
    public EvidenceNoteDto addNote(@PathVariable UUID id,
                                   @Valid @RequestBody AddEvidenceNoteRequest req,
                                   @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.addNote(id, req.note(), me != null ? me.id() : null);
    }

    @GetMapping("/evidence/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER')")
    public List<EvidenceNoteDto> listNotes(@PathVariable UUID id,
                                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listNotes(id, me != null ? me.id() : null);
    }
}
