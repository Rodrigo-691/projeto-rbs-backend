package com.rbs.planeta.controller;

import com.rbs.planeta.dto.SignedUrlResponse;
import com.rbs.planeta.model.SponsorFile;
import com.rbs.planeta.security.UserPrincipal;
import com.rbs.planeta.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Arquivos & Documentos", description = "Uploads de MIV, planos comerciais, contratos e assinaturas")
public class FileController {

    private final FileService fileService;

    @GetMapping("/sponsors/{sponsorId}/files")
    @Operation(summary = "Listar arquivos do patrocinador por aba ou categoria")
    public ResponseEntity<List<SponsorFile>> listFiles(
            @PathVariable UUID sponsorId,
            @RequestParam(required = false) String tabKey,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(fileService.listBySponsorAndTab(sponsorId, tabKey, category));
    }

    @PostMapping("/sponsors/{sponsorId}/files")
    @Operation(summary = "Registrar metadados de arquivo enviado")
    public ResponseEntity<SponsorFile> registerFile(
            @PathVariable UUID sponsorId,
            @RequestParam String tabKey,
            @RequestParam String category,
            @RequestParam String fileName,
            @RequestParam String filePath,
            @RequestParam Long fileSize,
            @RequestParam(required = false) String fileType,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        SponsorFile saved = fileService.registerFile(
                sponsorId, tabKey, category, fileName, filePath, fileSize, fileType, user.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/sponsors/{sponsorId}/files/{fileId}")
    @Operation(summary = "Remover arquivo do portal")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID sponsorId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        fileService.deleteFile(sponsorId, fileId, user.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/{fileId}/download-url")
    @Operation(summary = "Obter URL assinada e segura para download direto do arquivo")
    public ResponseEntity<SignedUrlResponse> getDownloadUrl(@PathVariable UUID fileId) {
        return ResponseEntity.ok(fileService.getSignedDownloadUrl(fileId));
    }
}
