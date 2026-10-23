package com.etra.cti.application.apikey;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.application.apikey.dto.ApiKeyCreatedResponse;
import com.etra.cti.application.apikey.dto.ApiKeyDto;
import com.etra.cti.application.apikey.dto.CreateApiKeyRequest;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.OrganizationService;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationApiKeyService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApplicationApiKeyRepository apiKeyRepo;
    private final ApplicationRepository appRepo;
    private final ProjectRepository projectRepo;
    private final OrganizationService orgService;
    private final AuditService auditService;

    public ApplicationApiKeyService(ApplicationApiKeyRepository apiKeyRepo,
                                    ApplicationRepository appRepo,
                                    ProjectRepository projectRepo,
                                    OrganizationService orgService,
                                    AuditService auditService) {
        this.apiKeyRepo = apiKeyRepo;
        this.appRepo = appRepo;
        this.projectRepo = projectRepo;
        this.orgService = orgService;
        this.auditService = auditService;
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID applicationId, CreateApiKeyRequest req, UUID userId) {
        Application app = appRepo.findById(applicationId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projectRepo.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), userId);

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String hexSecret = HexFormat.of().formatHex(randomBytes);
        String rawKey = "etra_live_" + hexSecret;
        String prefix = rawKey.substring(0, 16);
        String hash = sha256(rawKey);

        String keyName = (req.name() != null && !req.name().isBlank())
            ? req.name().trim()
            : (app.getName() + " Ingestion Key");

        ApplicationApiKey apiKey = new ApplicationApiKey();
        apiKey.setApplicationId(applicationId);
        apiKey.setName(keyName);
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(hash);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setCreatedBy(userId);

        if (req.expiryDays() != null && req.expiryDays() > 0) {
            apiKey.setExpiresAt(Instant.now().plus(req.expiryDays(), ChronoUnit.DAYS));
        }

        apiKeyRepo.save(apiKey);

        auditService.record(userId, "API_KEY_CREATED", "Application", applicationId.toString(),
            Map.of("keyId", apiKey.getId().toString(), "keyPrefix", prefix, "name", keyName));

        log.info(">>> Issued new API key {} ({}) for application {}", prefix, keyName, applicationId);
        return ApiKeyCreatedResponse.of(apiKey, rawKey);
    }

    public List<ApiKeyDto> listKeys(UUID applicationId, UUID userId) {
        Application app = appRepo.findById(applicationId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projectRepo.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), userId);

        return apiKeyRepo.findByApplicationIdOrderByCreatedAtDesc(applicationId)
            .stream()
            .map(ApiKeyDto::from)
            .toList();
    }

    @Transactional
    public ApiKeyDto revokeApiKey(UUID applicationId, UUID keyId, UUID userId) {
        Application app = appRepo.findById(applicationId)
            .orElseThrow(() -> new NotFoundException("Application not found"));
        Project project = projectRepo.findById(app.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));
        orgService.requireMember(project.getOrganizationId(), userId);

        ApplicationApiKey key = apiKeyRepo.findByIdAndApplicationId(keyId, applicationId)
            .orElseThrow(() -> new NotFoundException("API key not found for application"));

        key.setStatus(ApiKeyStatus.REVOKED);
        key.setRevokedAt(Instant.now());
        apiKeyRepo.save(key);

        auditService.record(userId, "API_KEY_REVOKED", "Application", applicationId.toString(),
            Map.of("keyId", keyId.toString(), "keyPrefix", key.getKeyPrefix()));

        log.info(">>> Revoked API key {} for application {}", key.getKeyPrefix(), applicationId);
        return ApiKeyDto.from(key);
    }

    @Transactional
    public Optional<ApplicationApiKey> validateApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String cleanKey = rawKey.trim();
        if (cleanKey.startsWith("Bearer ")) {
            cleanKey = cleanKey.substring(7).trim();
        }
        String hash = sha256(cleanKey);
        Optional<ApplicationApiKey> optKey = apiKeyRepo.findByKeyHashAndStatus(hash, ApiKeyStatus.ACTIVE);
        if (optKey.isEmpty()) {
            return Optional.empty();
        }
        ApplicationApiKey key = optKey.get();
        if (key.isExpired()) {
            key.setStatus(ApiKeyStatus.EXPIRED);
            apiKeyRepo.save(key);
            return Optional.empty();
        }
        return Optional.of(key);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
