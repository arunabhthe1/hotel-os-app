package com.hotelos.security;

import com.hotelos.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(padSecret(properties.secret()).getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.expirationMs());
        return Jwts.builder()
                .subject(principal.getPublicId())
                .claim("email", principal.getUsername())
                .claim("role", principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String parseSubject(String token) {
        return parse(token).getSubject();
    }

    public long expirationMs() {
        return properties.expirationMs();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid or expired token", ex);
        }
    }

    private static String padSecret(String secret) {
        if (secret.length() >= 32) {
            return secret;
        }
        return secret + "0".repeat(32 - secret.length());
    }
}
