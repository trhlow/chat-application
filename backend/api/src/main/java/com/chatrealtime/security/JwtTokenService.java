package com.chatrealtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtTokenService {
    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.accessExpirationMs());

        return Jwts.builder()
                .subject(principal.getId())
                .claim("username", principal.getUsername())
                .claim("tokenVersion", principal.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public int extractTokenVersion(String token) {
        Object tokenVersion = parseClaims(token).get("tokenVersion");
        if (tokenVersion instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    /**
     * Parses and verifies the token once; use from HTTP filters to avoid duplicate JWT work.
     */
    public Optional<Claims> parseValidClaims(String token) {
        try {
            Claims claims = parseClaims(token);
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public boolean isTokenValid(String token) {
        return parseValidClaims(token).isPresent();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

