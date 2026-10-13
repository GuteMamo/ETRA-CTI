package com.etra.cti.detection;

import com.etra.cti.event.SecurityEvent;

import java.util.Optional;

public interface DetectionRule {
    String id();
    Optional<Detection> evaluate(SecurityEvent event);
}
