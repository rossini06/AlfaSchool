package br.com.alfaschool.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UUID userId, UUID tenantId, UUID unitId, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.jwtExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("unitId", unitId == null ? null : unitId.toString())
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.refreshExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("roles", List.of())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseToken(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseToken(token).get("type", String.class));
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
