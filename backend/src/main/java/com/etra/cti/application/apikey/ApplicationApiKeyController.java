package com.etra.cti.application.apikey;

import com.etra.cti.application.apikey.dto.ApiKeyCreatedResponse;
import com.etra.cti.application.apikey.dto.ApiKeyDto;
import com.etra.cti.application.apikey.dto.CreateApiKeyRequest;
import com.etra.cti.auth.JwtAuthFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{applicationId}/api-keys")
public class ApplicationApiKeyController {

    private final ApplicationApiKeyService apiKeyService;

    public ApplicationApiKeyController(ApplicationApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("!hasRole('VIEWER')")
    public ApiKeyCreatedResponse createApiKey(@PathVariable UUID applicationId,
                                              @Valid @RequestBody(required = false) CreateApiKeyRequest req,
                                              @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        CreateApiKeyRequest effectiveReq = (req != null) ? req : new CreateApiKeyRequest(null, 30);
        return apiKeyService.createApiKey(applicationId, effectiveReq, me.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER','PENTESTER','ANALYST','DEVELOPER','VIEWER')")
    public List<ApiKeyDto> listApiKeys(@PathVariable UUID applicationId,
                                       @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return apiKeyService.listKeys(applicationId, me.id());
    }

    @PostMapping("/{keyId}/revoke")
    @PreAuthorize("!hasRole('VIEWER')")
    public ApiKeyDto revokeApiKeyPost(@PathVariable UUID applicationId,
                                      @PathVariable UUID keyId,
                                      @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return apiKeyService.revokeApiKey(applicationId, keyId, me.id());
    }

    @DeleteMapping("/{keyId}")
    @PreAuthorize("!hasRole('VIEWER')")
    public ApiKeyDto revokeApiKeyDelete(@PathVariable UUID applicationId,
                                        @PathVariable UUID keyId,
                                        @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return apiKeyService.revokeApiKey(applicationId, keyId, me.id());
    }
}
