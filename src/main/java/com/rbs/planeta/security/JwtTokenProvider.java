package com.rbs.planeta.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${supabase.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("Token JWT inválido ou expirado: {}", ex.getMessage());
            return false;
        }
    }

    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String sub = claims.getSubject();
        UUID userId = sub != null ? UUID.fromString(sub) : UUID.randomUUID();
        String email = claims.get("email", String.class);

        // Check if user has admin role in app metadata
        boolean isAdmin = false;
        Object appMetadata = claims.get("app_metadata");
        if (appMetadata instanceof java.util.Map<?, ?> map) {
            Object role = map.get("role");
            isAdmin = "admin_rbs".equalsIgnoreCase(String.valueOf(role));
        }

        return UserPrincipal.create(userId, email, isAdmin);
    }
}
