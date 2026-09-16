package com.etra.cti.auth;

import com.etra.cti.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;
    private final long accessExpiry;

    public JwtService(
        @Value("${etra.jwt.secret}") String secret,
        @Value("${etra.jwt.access-token-expiry}") long accessExpiry
    ) {
        this.secret = secret;
        this.accessExpiry = accessExpiry;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getId().toString())
            .claims(Map.of(
                "email", user.getEmail(),
                "role",  user.getRole().name(),
                "name",  user.getFullName()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessExpiry)))
            .signWith(key())
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload();
    }
}
