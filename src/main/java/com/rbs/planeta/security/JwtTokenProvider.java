package com.rbs.planeta.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JdbcTemplate jdbcTemplate;

    @Value("${supabase.jwt.secret:}")
    private String jwtSecret;

    @Value("${supabase.url:https://tsnolyvatlustxpqfrhg.supabase.co}")
    private String supabaseUrl;

    private final Map<String, PublicKey> ecPublicKeys = new ConcurrentHashMap<>();
    private PublicKey defaultEcPublicKey;

    @PostConstruct
    public void init() {
        // Chave pública ES256 oficial do projeto Supabase tsnolyvatlustxpqfrhg
        try {
            PublicKey fallbackKey = buildEcPublicKey(
                    "Xaknjr14QAq2xQr0HHx2COp9FylNm7bKTCzaT-VAG2A",
                    "5fjD-HqE4zjhmeav3PXHQYevR10H1hU2gCj2HnfPObg"
            );
            ecPublicKeys.put("2eb90c31-4ebd-406d-8bfd-ef50e40abe8b", fallbackKey);
            this.defaultEcPublicKey = fallbackKey;
            log.info("Chave pública ES256 estática do Supabase carregada com sucesso.");
        } catch (Exception e) {
            log.warn("Não foi possível inicializar chave ES256 estática: {}", e.getMessage());
        }

        // Tentar buscar JWKS dinâmico do Supabase
        fetchJwksKeysAsync();
    }

    private void fetchJwksKeysAsync() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(jwksUrl))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(resp.body());
                    JsonNode keys = root.get("keys");
                    if (keys != null && keys.isArray()) {
                        for (JsonNode k : keys) {
                            String kty = k.path("kty").asText();
                            String kid = k.path("kid").asText();
                            String x = k.path("x").asText();
                            String y = k.path("y").asText();

                            if ("EC".equalsIgnoreCase(kty) && !x.isBlank() && !y.isBlank()) {
                                PublicKey pk = buildEcPublicKey(x, y);
                                ecPublicKeys.put(kid, pk);
                                if (defaultEcPublicKey == null) {
                                    defaultEcPublicKey = pk;
                                }
                            }
                        }
                        log.info("JWKS atualizado dinamicamente a partir do Supabase: {} chave(s) registradas.", ecPublicKeys.size());
                    }
                }
            } catch (Exception e) {
                log.debug("Consulta ao JWKS do Supabase finalizada (utilizando chave em cache): {}", e.getMessage());
            }
        });
    }

    private PublicKey buildEcPublicKey(String xBase64Url, String yBase64Url) throws Exception {
        byte[] xBytes = Base64.getUrlDecoder().decode(xBase64Url);
        byte[] yBytes = Base64.getUrlDecoder().decode(yBase64Url);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);
        ECPoint point = new ECPoint(x, y);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(new ECPublicKeySpec(point, spec));
    }

    private Key resolveKey(Header header) {
        String alg = header.getAlgorithm();
        Object kidObj = header.get("kid");
        String kid = kidObj != null ? String.valueOf(kidObj) : null;

        if ("ES256".equalsIgnoreCase(alg)) {
            if (kid != null && ecPublicKeys.containsKey(kid)) {
                return ecPublicKeys.get(kid);
            }
            if (defaultEcPublicKey != null) {
                return defaultEcPublicKey;
            }
            if (!ecPublicKeys.isEmpty()) {
                return ecPublicKeys.values().iterator().next();
            }
            throw new SecurityException("Nenhuma chave pública ES256 encontrada para validar o token.");
        }

        // Fallback para HS256 com segredo simétrico
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            try {
                if (jwtSecret.endsWith("=")) {
                    return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
                }
            } catch (Exception ignored) {
            }
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }

        throw new SecurityException("Algoritmo JWT não suportado ou chave não configurada: " + alg);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .keyLocator(this::resolveKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("Token JWT inválido ou rejeitado: {}", ex.getMessage());
            return false;
        }
    }

    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = Jwts.parser()
                .keyLocator(this::resolveKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String sub = claims.getSubject();
        UUID userId = sub != null ? UUID.fromString(sub) : UUID.randomUUID();
        String email = claims.get("email", String.class);

        boolean isAdmin = checkIsAdmin(claims, userId, email);

        return UserPrincipal.create(userId, email, isAdmin);
    }

    private boolean checkIsAdmin(Claims claims, UUID userId, String email) {
        // 1. Checagem em app_metadata / user_metadata no JWT
        Object appMetadata = claims.get("app_metadata");
        if (appMetadata instanceof Map<?, ?> map) {
            Object role = map.get("role");
            if ("admin_rbs".equalsIgnoreCase(String.valueOf(role))) {
                return true;
            }
        }

        Object userMetadata = claims.get("user_metadata");
        if (userMetadata instanceof Map<?, ?> map) {
            Object role = map.get("role");
            if ("admin_rbs".equalsIgnoreCase(String.valueOf(role))) {
                return true;
            }
        }

        // 2. Checagem direta no banco de dados (tabelas user_roles e admin_emails)
        try {
            if (userId != null) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = 'admin_rbs'",
                        Integer.class,
                        userId
                );
                if (count != null && count > 0) {
                    return true;
                }
            }

            if (email != null && !email.isBlank()) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM admin_emails WHERE LOWER(email) = LOWER(?)",
                        Integer.class,
                        email.trim()
                );
                if (count != null && count > 0) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.warn("Erro ao consultar user_roles/admin_emails no banco: {}", ex.getMessage());
        }

        return false;
    }
}
