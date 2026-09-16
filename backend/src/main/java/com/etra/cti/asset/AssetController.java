package com.etra.cti.asset;

import com.etra.cti.asset.dto.AssetDto;
import com.etra.cti.asset.dto.CreateAssetRequest;
import com.etra.cti.auth.JwtAuthFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService service;

    @PostMapping("/applications/{appId}/assets")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetDto create(@PathVariable UUID appId,
                           @Valid @RequestBody CreateAssetRequest req,
                           @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.create(appId, req, me.id());
    }

    @GetMapping("/applications/{appId}/assets")
    public List<AssetDto> list(@PathVariable UUID appId,
                               @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        return service.list(appId, me.id());
    }
}
