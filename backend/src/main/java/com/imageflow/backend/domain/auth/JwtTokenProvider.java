package com.imageflow.backend.domain.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.imageflow.backend.common.exception.UnauthorizedException;
import com.imageflow.backend.domain.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.expiration-seconds:604800}") long expirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public UUID parseUserId(String authorizationHeader) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(extractBearerToken(authorizationHeader))
                    .getPayload();

            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("invalid access token");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("authorization header is required");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new UnauthorizedException("authorization header must use Bearer token");
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("access token is required");
        }
        return token;
    }
}
