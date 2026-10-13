package com.etra.cti.threatintel;

import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.threatintel.dto.CreateIocRequest;
import com.etra.cti.threatintel.dto.IocDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);

    private final IocRepository iocs;
    private final AuditService audit;

    public ThreatIntelService(IocRepository iocs, AuditService audit) {
        this.iocs = iocs;
        this.audit = audit;
    }

    @Transactional
    public IocDto add(CreateIocRequest req, UUID userId) {
        if (iocs.existsByTypeAndValue(req.type(), req.value())) {
            throw new IllegalArgumentException("IOC already exists: " + req.type() + "=" + req.value());
        }
        Ioc ioc = new Ioc();
        ioc.setType(req.type());
        ioc.setValue(req.value().trim());
        ioc.setDescription(req.description());
        ioc.setSource(req.source());
        ioc.setSeverity(req.severity() == null ? com.etra.cti.detection.Severity.HIGH : req.severity());
        ioc.setActive(true);
        ioc.setExpiresAt(req.expiresAt());
        ioc.setCreatedBy(userId);
        iocs.save(ioc);

        audit.record(userId, "IOC_ADDED", "Ioc", ioc.getId().toString(),
            Map.of("type", req.type().name(), "value", req.value()));

        log.info(">>> IOC added: {}={}", req.type(), req.value());
        return IocDto.from(ioc);
    }

    public List<IocDto> list() {
        return iocs.findByActiveTrueOrderByCreatedAtDesc().stream().map(IocDto::from).toList();
    }

    public List<IocDto> listByType(IocType type) {
        return iocs.findByTypeAndActiveTrueOrderByCreatedAtDesc(type).stream().map(IocDto::from).toList();
    }

    @Transactional
    public void deactivate(UUID id) {
        Ioc i = iocs.findById(id).orElseThrow(() -> new NotFoundException("IOC not found"));
        i.setActive(false);
        iocs.save(i);
    }

    /**
     * Match a single value against the IOC list.
     */
    public Optional<Ioc> match(IocType type, String value) {
        if (value == null) return Optional.empty();
        return iocs.findByTypeAndValue(type, value.trim())
            .filter(Ioc::getActive);
    }
}
