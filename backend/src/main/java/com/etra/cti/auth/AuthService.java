package com.etra.cti.auth;

import com.etra.cti.auth.dto.*;
import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import com.etra.cti.user.UserRole;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

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
        u.setRole(UserRole.VIEWER);
        u.setEnabled(true);
        users.save(u);
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

        rt.setRevoked(true);
        refreshTokens.save(rt);

        return new TokenPair(jwt.generateAccessToken(u), issueRefreshToken(u));
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
