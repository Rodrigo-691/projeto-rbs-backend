package com.rbs.planeta.controller;

import com.rbs.planeta.dto.MediaImportResultDto;
import com.rbs.planeta.dto.MediaItemDto;
import com.rbs.planeta.security.UserPrincipal;
import com.rbs.planeta.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Mídia & Conteúdo", description = "Veiculações publicitárias, inserções e importação de planilhas")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/sponsors/{sponsorId}/media")
    @Operation(summary = "Listar inserções de mídia de um patrocinador")
    public ResponseEntity<List<MediaItemDto>> listMedia(@PathVariable UUID sponsorId) {
        return ResponseEntity.ok(mediaService.listBySponsor(sponsorId));
    }

    @PostMapping("/sponsors/{sponsorId}/media")
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Publicar nova veiculação de mídia")
    public ResponseEntity<MediaItemDto> createMedia(
            @PathVariable UUID sponsorId,
            @Valid @RequestBody MediaItemDto dto,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.create(sponsorId, dto, user.getEmail()));
    }

    @DeleteMapping("/sponsors/{sponsorId}/media/{mediaId}")
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Excluir veiculação de mídia")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable UUID sponsorId,
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        mediaService.delete(sponsorId, mediaId, user.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/media/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN_RBS')")
    @Operation(summary = "Importar planilha Excel de veiculações de mídia com Apache POI")
    public ResponseEntity<MediaImportResultDto> importExcel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        MediaImportResultDto result = mediaService.importSpreadsheet(file, user.getEmail());
        return ResponseEntity.ok(result);
    }
}
