package com.proflow.security;

import com.proflow.config.ApplicationProperties;
import com.proflow.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final ApplicationProperties applicationProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(applicationProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @PostConstruct
    public void validateSigningKey() {
        try {
            getSigningKey();
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET must be valid Base64 encoding of at least 32 bytes", ex);
        }
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole());
        if (user.getCompany() != null) {
            claims.put("company", user.getCompany().getId());
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + applicationProperties.getJwt().getExpirationMs());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateToken(UserPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", principal.getId());
        claims.put("role", principal.getRole());
        if (principal.getCompanyId() != null) {
            claims.put("company", principal.getCompanyId());
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + applicationProperties.getJwt().getExpirationMs());

        return Jwts.builder()
                .claims(claims)
                .subject(principal.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateResetPasswordToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("type", "reset-password");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 10 * 60 * 1000); // 10 minutes

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object idObj = getClaims(token).get("id");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        }
        return Long.parseLong(idObj.toString());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }
}
