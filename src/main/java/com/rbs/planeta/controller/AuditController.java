package com.rbs.planeta.controller;

import com.rbs.planeta.dto.AuditLogDto;
import com.rbs.planeta.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auditoria & Histórico", description = "Trilhas de auditoria e atividades em tempo real")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/audit/recent")
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Últimas movimentações do portal (apenas Admin RBS)")
    public ResponseEntity<List<AuditLogDto>> listRecent(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditService.listRecent(Math.min(limit, 50)));
    }

    @GetMapping("/sponsors/{sponsorId}/audit")
    @Operation(summary = "Histórico de atualizações de uma marca parceira")
    public ResponseEntity<List<AuditLogDto>> listBySponsor(
            @PathVariable UUID sponsorId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(auditService.listBySponsor(sponsorId, Math.min(limit, 200)));
    }
}
