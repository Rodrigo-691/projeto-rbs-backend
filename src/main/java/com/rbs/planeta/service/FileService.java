package com.rbs.planeta.service;

import com.rbs.planeta.dto.SignedUrlResponse;
import com.rbs.planeta.model.Sponsor;
import com.rbs.planeta.model.SponsorFile;
import com.rbs.planeta.repository.SponsorFileRepository;
import com.rbs.planeta.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final SponsorFileRepository fileRepository;
    private final SponsorRepository sponsorRepository;
    private final AuditService auditService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.bucket:sponsor-files}")
    private String bucketName;

    @Transactional(readOnly = true)
    public List<SponsorFile> listBySponsorAndTab(UUID sponsorId, String tabKey, String category) {
        if (category != null && !category.isBlank()) {
            return fileRepository.findBySponsorIdAndTabKeyAndCategoryOrderByCreatedAtDesc(sponsorId, tabKey, category);
        }
        if (tabKey != null && !tabKey.isBlank()) {
            return fileRepository.findBySponsorIdAndTabKeyOrderByCreatedAtDesc(sponsorId, tabKey);
        }
        return fileRepository.findBySponsorIdOrderByCreatedAtDesc(sponsorId);
    }

    @Transactional
    public SponsorFile registerFile(UUID sponsorId, String tabKey, String category, String fileName, String filePath, Long fileSize, String fileType, String actorEmail) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Patrocinador não encontrado: " + sponsorId));

        SponsorFile file = SponsorFile.builder()
                .sponsor(sponsor)
                .tabKey(tabKey)
                .category(category)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileType(fileType)
                .uploaderEmail(actorEmail)
                .build();

        SponsorFile saved = fileRepository.save(file);
        auditService.log(sponsorId, actorEmail, "INSERT", "sponsor_files", "Enviou arquivo: " + fileName + " (" + category + ")");
        return saved;
    }

    @Transactional
    public void deleteFile(UUID sponsorId, UUID fileId, String actorEmail) {
        SponsorFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado: " + fileId));

        if (!file.getSponsor().getId().equals(sponsorId)) {
            throw new RuntimeException("Arquivo não pertence ao patrocinador informado.");
        }

        fileRepository.delete(file);
        auditService.log(sponsorId, actorEmail, "DELETE", "sponsor_files", "Excluiu arquivo: " + file.getFileName() + " (" + file.getCategory() + ")");
    }

    /**
     * Generates a direct download signed URL for private storage
     */
    public SignedUrlResponse getSignedDownloadUrl(UUID fileId) {
        SponsorFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado: " + fileId));

        // URL format pointing to Supabase storage signed endpoint
        String signedUrl = String.format("%s/storage/v1/object/sign/%s/%s", supabaseUrl, bucketName, file.getFilePath());

        return SignedUrlResponse.builder()
                .path(file.getFilePath())
                .url(signedUrl)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
    }
}
