package com.rbs.planeta.controller;

import com.rbs.planeta.dto.SponsorDto;
import com.rbs.planeta.model.QuotaLevel;
import com.rbs.planeta.security.UserPrincipal;
import com.rbs.planeta.service.SponsorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
@Tag(name = "Patrocinadores", description = "Gestão de marcas patrocinadoras, cotas e dados cadastrais")
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping
    @Operation(summary = "Listar marcas parceiras do usuário autenticado")
    public ResponseEntity<List<SponsorDto>> listSponsors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QuotaLevel quota,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user.isAdmin()) {
            return ResponseEntity.ok(sponsorService.listAll(search, quota));
        }
        return ResponseEntity.ok(sponsorService.listForUser(user.getEmail(), false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de um patrocinador por ID")
    public ResponseEntity<SponsorDto> getSponsorById(@PathVariable UUID id) {
        return ResponseEntity.ok(sponsorService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Cadastrar novo patrocinador (apenas Admin RBS)")
    public ResponseEntity<SponsorDto> createSponsor(
            @Valid @RequestBody SponsorDto dto,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        SponsorDto created = sponsorService.create(dto, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Atualizar perfil cadastral do patrocinador (apenas Admin RBS)")
    public ResponseEntity<SponsorDto> updateSponsor(
            @PathVariable UUID id,
            @Valid @RequestBody SponsorDto dto,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        SponsorDto updated = sponsorService.update(id, dto, user.getEmail());
        return ResponseEntity.ok(updated);
    }
}
