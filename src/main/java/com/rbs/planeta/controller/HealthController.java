package com.rbs.planeta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Monitoramento", description = "Status do serviço e checagem de integridade")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verificar status e saúde da API")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "planeta-backend",
                "version", "1.0.0",
                "timestamp", OffsetDateTime.now()
        ));
    }
}
