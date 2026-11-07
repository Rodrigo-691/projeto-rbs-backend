package com.rbs.planeta.service;

import com.rbs.planeta.dto.AuditLogDto;
import com.rbs.planeta.model.AuditLog;
import com.rbs.planeta.model.Sponsor;
import com.rbs.planeta.repository.AuditLogRepository;
import com.rbs.planeta.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SponsorRepository sponsorRepository;

    @Transactional
    public void log(UUID sponsorId, String actorEmail, String action, String tableName, String summary) {
        AuditLog entry = AuditLog.builder()
                .sponsorId(sponsorId)
                .actorEmail(actorEmail != null ? actorEmail : "Sistema")
                .action(action)
                .tableName(tableName)
                .summary(summary)
                .build();
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> listRecent(int limit) {
        List<AuditLog> list = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        Map<UUID, String> sponsorNames = sponsorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Sponsor::getId, Sponsor::getName, (s1, s2) -> s1));

        return list.stream().map(a -> AuditLogDto.builder()
                .id(a.getId())
                .sponsorId(a.getSponsorId())
                .sponsorName(a.getSponsorId() != null ? sponsorNames.get(a.getSponsorId()) : null)
                .actorEmail(a.getActorEmail())
                .action(a.getAction())
                .summary(a.getSummary())
                .tableName(a.getTableName())
                .createdAt(a.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> listBySponsor(UUID sponsorId, int limit) {
        List<AuditLog> list = auditLogRepository.findBySponsorIdOrderByCreatedAtDesc(sponsorId, PageRequest.of(0, limit));
        return list.stream().map(a -> AuditLogDto.builder()
                .id(a.getId())
                .sponsorId(a.getSponsorId())
                .actorEmail(a.getActorEmail())
                .action(a.getAction())
                .summary(a.getSummary())
                .tableName(a.getTableName())
                .createdAt(a.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }
}
