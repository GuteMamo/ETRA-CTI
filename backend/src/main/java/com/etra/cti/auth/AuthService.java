package com.etra.cti.auth;

import com.etra.cti.audit.AuditService;
import com.etra.cti.auth.dto.*;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import com.etra.cti.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       AuditService audit) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    @Value("${etra.jwt.refresh-token-expiry}")
    private long refreshExpiryMs;

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User u = new User();
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName());
        UserRole assignedRole = req.role() != null ? req.role()
            : (users.count() == 0 ? UserRole.ADMIN : UserRole.ANALYST);
        u.setRole(assignedRole);
        u.setEnabled(true);
        users.save(u);

        audit.record(u.getId(), "USER_REGISTERED", "User", u.getId().toString(),
            Map.of("email", u.getEmail(), "role", u.getRole().name()));

        return UserDto.from(u);
    }

    @Transactional
    public TokenPair login(LoginRequest req) {
        User u = users.findByEmail(req.email().toLowerCase())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!u.isEnabled()) throw new BadCredentialsException("Account disabled");
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String access  = jwt.generateAccessToken(u);
        String refresh = issueRefreshToken(u);

        audit.record(u.getId(), "USER_LOGGED_IN", "User", u.getId().toString(),
            Map.of("email", u.getEmail(), "role", u.getRole().name()));

        return new TokenPair(access, refresh);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken rt = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        User u = users.findById(rt.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (!u.isEnabled()) {
            throw new BadCredentialsException("Account disabled");
        }

        rt.setRevoked(true);
        refreshTokens.save(rt);

        return new TokenPair(jwt.generateAccessToken(u), issueRefreshToken(u));
    }

    @Transactional
    public void logout(UUID userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = sha256(rawRefreshToken);
            refreshTokens.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokens.save(rt);
            });
        }
        if (userId != null) {
            List<RefreshToken> active = refreshTokens.findByUserIdAndRevokedFalse(userId);
            for (RefreshToken rt : active) {
                rt.setRevoked(true);
            }
            refreshTokens.saveAll(active);

            audit.record(userId, "USER_LOGGED_OUT", "User", userId.toString(), Map.of());
        }
    }

    private String issueRefreshToken(User u) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(u.getId());
        rt.setTokenHash(sha256(raw));
        rt.setExpiresAt(Instant.now().plus(Duration.ofMillis(refreshExpiryMs)));
        refreshTokens.save(rt);
        return raw;
    }

    private String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
