package com.etra.cti.detection;

import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.detection.dto.DetectionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionRepository detectionRepo;

    public DetectionController(DetectionRepository detectionRepo) {
        this.detectionRepo = detectionRepo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public Page<DetectionDto> list(
        @RequestParam(required = false) UUID eventId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) Severity severity,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        if (eventId != null) {
            return detectionRepo.findByEventId(eventId, pageable).map(DetectionDto::from);
        } else if (type != null) {
            return detectionRepo.findByType(type, pageable).map(DetectionDto::from);
        } else if (severity != null) {
            return detectionRepo.findBySeverity(severity, pageable).map(DetectionDto::from);
        } else {
            return detectionRepo.findAllByOrderByCreatedAtDesc(pageable).map(DetectionDto::from);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public DetectionDto get(@PathVariable UUID id) {
        Detection d = detectionRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Detection not found"));
        return DetectionDto.from(d);
    }
}
