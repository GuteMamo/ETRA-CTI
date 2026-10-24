package com.etra.cti.auth;

import com.etra.cti.application.apikey.ApplicationApiKey;
import com.etra.cti.application.apikey.ApplicationApiKeyService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final ApplicationApiKeyService apiKeyService;

    public JwtAuthFilter(JwtService jwt, ApplicationApiKeyService apiKeyService) {
        this.jwt = jwt;
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest req,
        @NonNull HttpServletResponse res,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        // 1. Check for dedicated application API key headers (X-ETRA-API-KEY, X-API-KEY, API-KEY)
        String apiKeyHeader = req.getHeader("X-ETRA-API-KEY");
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            apiKeyHeader = req.getHeader("X-API-KEY");
        }
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            apiKeyHeader = req.getHeader("API-KEY");
        }
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            apiKeyHeader = req.getParameter("apiKey");
        }
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            apiKeyHeader = req.getParameter("api_key");
        }

        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            Optional<ApplicationApiKey> optKey = apiKeyService.validateApiKey(apiKeyHeader.trim());
            if (optKey.isPresent()) {
                ApplicationApiKey key = optKey.get();
                var auth = new UsernamePasswordAuthenticationToken(
                    new AuthedUser(key.getApplicationId(), "app:" + key.getName(), "APPLICATION"),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_APPLICATION"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                chain.doFilter(req, res);
                return;
            }
        }

        // 2. Check Authorization header (could be Bearer JWT, Bearer etra_live_..., ApiKey etra_live_..., or raw etra_live_...)
        String header = req.getHeader("Authorization");
        if (header != null && !header.isBlank()) {
            String token = header.trim();
            if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            } else if (token.regionMatches(true, 0, "ApiKey ", 0, 7)) {
                token = token.substring(7).trim();
            } else if (token.regionMatches(true, 0, "Token ", 0, 6)) {
                token = token.substring(6).trim();
            }

            if (token.startsWith("etra_live_")) {
                Optional<ApplicationApiKey> optKey = apiKeyService.validateApiKey(token);
                if (optKey.isPresent()) {
                    ApplicationApiKey key = optKey.get();
                    var auth = new UsernamePasswordAuthenticationToken(
                        new AuthedUser(key.getApplicationId(), "app:" + key.getName(), "APPLICATION"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_APPLICATION"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    chain.doFilter(req, res);
                    return;
                }
            } else {
                try {
                    Claims claims = jwt.parse(token);
                    String userId = claims.getSubject();
                    String role   = claims.get("role", String.class);
                    String email  = claims.get("email", String.class);

                    var auth = new UsernamePasswordAuthenticationToken(
                        new AuthedUser(UUID.fromString(userId), email, role),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {
                }
            }
        }
        chain.doFilter(req, res);
    }

    public record AuthedUser(UUID id, String email, String role) {}
}
