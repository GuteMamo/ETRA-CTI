package com.etra.cti.event;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.detection.dto.DetectionDto;
import com.etra.cti.detection.DetectionService;
import com.etra.cti.event.dto.EventDto;
import com.etra.cti.event.dto.IngestEventRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService service;
    private final DetectionService detectionService;

    public EventController(EventService service, DetectionService detectionService) {
        this.service = service;
        this.detectionService = detectionService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','APPLICATION')")
    public EventDto ingest(@Valid @RequestBody IngestEventRequest req,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.ingest(req, me);
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public Page<EventDto> list(
        @RequestParam(required = false) UUID applicationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        if (applicationId != null) {
            return service.listByApplication(applicationId, PageRequest.of(page, size));
        }
        return service.listAll(PageRequest.of(page, size));
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public EventDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/applications/{appId}/events")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public Page<EventDto> listForApp(@PathVariable UUID appId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.listByApplication(appId, PageRequest.of(page, size));
    }

    @GetMapping("/events/{id}/detections")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<DetectionDto> detectionsForEvent(@PathVariable UUID id,
                                                 @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return detectionService.forEvent(id).stream().map(DetectionDto::from).toList();
    }
}
