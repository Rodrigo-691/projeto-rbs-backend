package com.rbs.planeta.service;

import com.rbs.planeta.dto.SponsorDto;
import com.rbs.planeta.model.QuotaLevel;
import com.rbs.planeta.model.Sponsor;
import com.rbs.planeta.repository.MediaItemRepository;
import com.rbs.planeta.repository.SponsorFileRepository;
import com.rbs.planeta.repository.SponsorMemberRepository;
import com.rbs.planeta.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final SponsorMemberRepository memberRepository;
    private final SponsorFileRepository fileRepository;
    private final MediaItemRepository mediaRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SponsorDto> listAll(String query, QuotaLevel quota) {
        List<Sponsor> list;
        if (query != null && !query.isBlank()) {
            list = sponsorRepository.searchByNameOrSegment(query.trim());
        } else if (quota != null) {
            list = sponsorRepository.findByQuotaOrderByNameAsc(quota);
        } else {
            list = sponsorRepository.findAllByOrderByNameAsc();
        }

        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SponsorDto> listForUser(String email, boolean isAdmin) {
        if (isAdmin) {
            return listAll(null, null);
        }
        return sponsorRepository.findByMemberEmail(email).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SponsorDto getById(UUID id) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrocinador não encontrado: " + id));
        return toDto(sponsor);
    }

    @Transactional
    public SponsorDto create(SponsorDto dto, String actorEmail) {
        Sponsor sponsor = Sponsor.builder()
                .name(dto.getName())
                .quota(dto.getQuota() != null ? dto.getQuota() : QuotaLevel.SUBCOTA)
                .segment(dto.getSegment())
                .cnpj(dto.getCnpj())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .notes(dto.getNotes())
                .build();

        Sponsor saved = sponsorRepository.save(sponsor);

        auditService.log(saved.getId(), actorEmail, "INSERT", "sponsors", "Cadastrou patrocinador: " + saved.getName());
        return toDto(saved);
    }

    @Transactional
    public SponsorDto update(UUID id, SponsorDto dto, String actorEmail) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrocinador não encontrado: " + id));

        sponsor.setName(dto.getName());
        if (dto.getQuota() != null) sponsor.setQuota(dto.getQuota());
        sponsor.setSegment(dto.getSegment());
        sponsor.setCnpj(dto.getCnpj());
        sponsor.setContactName(dto.getContactName());
        sponsor.setContactPhone(dto.getContactPhone());
        sponsor.setNotes(dto.getNotes());

        Sponsor saved = sponsorRepository.save(sponsor);
        auditService.log(saved.getId(), actorEmail, "UPDATE", "sponsors", "Atualizou dados cadastrais de " + saved.getName());
        return toDto(saved);
    }

    public SponsorDto toDto(Sponsor s) {
        return SponsorDto.builder()
                .id(s.getId())
                .name(s.getName())
                .quota(s.getQuota())
                .segment(s.getSegment())
                .cnpj(s.getCnpj())
                .contactName(s.getContactName())
                .contactPhone(s.getContactPhone())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .membersCount(memberRepository.findBySponsorIdOrderByEmailAsc(s.getId()).size())
                .filesCount(fileRepository.countBySponsorId(s.getId()))
                .mediaCount(mediaRepository.countBySponsorId(s.getId()))
                .totalReach(mediaRepository.calculateTotalReachBySponsorId(s.getId()))
                .build();
    }
}
