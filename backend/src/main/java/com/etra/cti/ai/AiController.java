package com.etra.cti.ai;

import com.etra.cti.ai.dto.AiResultDto;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Object status() {
        return java.util.Map.of("enabled", service.isEnabled());
    }

    @PostMapping("/summarize/investigation/{id}")
    public AiResultDto summarizeInvestigation(@PathVariable UUID id) {
        return service.summarizeInvestigation(id);
    }

    @PostMapping("/recommend/finding/{id}")
    public AiResultDto recommendRemediation(@PathVariable UUID id) {
        return service.recommendRemediation(id);
    }
}
