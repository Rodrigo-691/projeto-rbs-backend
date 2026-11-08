package com.rbs.planeta.service;

import com.rbs.planeta.dto.SponsorBatchImportResultDto;
import com.rbs.planeta.dto.SponsorDto;
import com.rbs.planeta.dto.SponsorRowErrorDto;
import com.rbs.planeta.model.QuotaLevel;
import com.rbs.planeta.model.Sponsor;
import com.rbs.planeta.repository.MediaItemRepository;
import com.rbs.planeta.repository.SponsorFileRepository;
import com.rbs.planeta.repository.SponsorMemberRepository;
import com.rbs.planeta.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

    /**
     * Importa múltiplos patrocinadores a partir de uma planilha Excel (.xlsx) usando Apache POI.
     * Colunas esperadas: Nome, CNPJ, Segmento, Ponto Focal Comercial, Telefone/WhatsApp, Cota.
     * Colunas obrigatórias: Nome, CNPJ e Cota.
     */
    @Transactional
    public SponsorBatchImportResultDto importSpreadsheet(MultipartFile file, String actorEmail) {
        SponsorBatchImportResultDto result = SponsorBatchImportResultDto.builder()
                .success(true)
                .build();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                result.setSuccess(false);
                result.getErrors().add(SponsorRowErrorDto.builder()
                        .rowNumber(1)
                        .errorMessage("A planilha enviada está vazia ou não contém linhas de dados.")
                        .build());
                return result;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.setSuccess(false);
                result.getErrors().add(SponsorRowErrorDto.builder()
                        .rowNumber(1)
                        .errorMessage("A linha de cabeçalho (linha 1) não foi encontrada.")
                        .build());
                return result;
            }

            int colNome = -1;
            int colCnpj = -1;
            int colSegmento = -1;
            int colContato = -1;
            int colTelefone = -1;
            int colCota = -1;

            int lastCell = headerRow.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String header = normalizeText(getCellValueAsString(cell));
                if (header.contains("nome") || header.contains("marca") || header.contains("razao")) {
                    colNome = c;
                } else if (header.contains("cnpj") || header.contains("documento")) {
                    colCnpj = c;
                } else if (header.contains("segmento") || header.contains("ramo") || header.contains("setor")) {
                    colSegmento = c;
                } else if (header.contains("focal") || header.contains("contato") || header.contains("responsavel")) {
                    colContato = c;
                } else if (header.contains("telefone") || header.contains("whatsapp") || header.contains("celular") || header.contains("fone")) {
                    colTelefone = c;
                } else if (header.contains("cota") || header.contains("categoria")) {
                    colCota = c;
                }
            }

            List<String> missingHeaders = new ArrayList<>();
            if (colNome == -1) missingHeaders.add("Nome");
            if (colCnpj == -1) missingHeaders.add("CNPJ");
            if (colCota == -1) missingHeaders.add("Cota");

            if (!missingHeaders.isEmpty()) {
                result.setSuccess(false);
                result.getErrors().add(SponsorRowErrorDto.builder()
                        .rowNumber(1)
                        .errorMessage("Cabeçalho inválido. Colunas obrigatórias ausentes na planilha: " + String.join(", ", missingHeaders))
                        .build());
                return result;
            }

            int totalRows = 0;
            int successCount = 0;
            int errorCount = 0;

            // Busca patrocinadores cadastrados no banco para verificação de duplicidade
            List<Sponsor> existingDbSponsors = sponsorRepository.findAll();
            Map<String, Sponsor> existingByCnpj = new HashMap<>();
            Map<String, Sponsor> existingByName = new HashMap<>();
            for (Sponsor s : existingDbSponsors) {
                if (s.getCnpj() != null && !s.getCnpj().isBlank()) {
                    String d = extractDigits(s.getCnpj());
                    if (d.length() >= 8) existingByCnpj.put(d, s);
                }
                if (s.getName() != null && !s.getName().isBlank()) {
                    existingByName.put(normalizeText(s.getName()), s);
                }
            }

            // Mapas para verificação de duplicidade na própria planilha
            Map<String, Integer> seenSheetCnpjs = new HashMap<>();
            Map<String, Integer> seenSheetNames = new HashMap<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String nome = colNome != -1 ? getCellValueAsString(row.getCell(colNome)) : null;
                String cnpj = colCnpj != -1 ? getCellValueAsString(row.getCell(colCnpj)) : null;
                String segmento = colSegmento != -1 ? getCellValueAsString(row.getCell(colSegmento)) : null;
                String contato = colContato != -1 ? getCellValueAsString(row.getCell(colContato)) : null;
                String telefone = colTelefone != -1 ? getCellValueAsString(row.getCell(colTelefone)) : null;
                String cotaRaw = colCota != -1 ? getCellValueAsString(row.getCell(colCota)) : null;

                boolean isRowEmpty = (nome == null || nome.isBlank()) &&
                                     (cnpj == null || cnpj.isBlank()) &&
                                     (cotaRaw == null || cotaRaw.isBlank()) &&
                                     (segmento == null || segmento.isBlank());
                if (isRowEmpty) continue;

                totalRows++;
                List<String> rowValidationErrors = new ArrayList<>();

                if (nome == null || nome.trim().isBlank()) {
                    rowValidationErrors.add("O campo obrigatório 'Nome' não foi informado.");
                }

                if (cnpj == null || cnpj.trim().isBlank()) {
                    rowValidationErrors.add("O campo obrigatório 'CNPJ' não foi informado.");
                }

                QuotaLevel quota = null;
                if (cotaRaw == null || cotaRaw.trim().isBlank()) {
                    rowValidationErrors.add("O campo obrigatório 'Cota' não foi informado.");
                } else {
                    quota = parseQuotaLevel(cotaRaw);
                    if (quota == null) {
                        rowValidationErrors.add("Cota inválida ('" + cotaRaw.trim() + "'). Valores válidos: Master, Subcota G ou Subcota.");
                    }
                }

                if (!rowValidationErrors.isEmpty()) {
                    errorCount++;
                    result.getErrors().add(SponsorRowErrorDto.builder()
                            .rowNumber(r + 1)
                            .sponsorName(nome != null && !nome.isBlank() ? nome.trim() : "(Não informado)")
                            .cnpj(cnpj != null && !cnpj.isBlank() ? cnpj.trim() : "(Não informado)")
                            .errorMessage(String.join(" ", rowValidationErrors))
                            .build());
                    continue;
                }

                String cleanCnpjDigits = extractDigits(cnpj);
                String cleanNameNorm = normalizeText(nome);

                // 1. Checagem contra o banco de dados (já cadastrado na plataforma)
                if (cleanCnpjDigits.length() >= 8 && existingByCnpj.containsKey(cleanCnpjDigits)) {
                    Sponsor existing = existingByCnpj.get(cleanCnpjDigits);
                    errorCount++;
                    result.getErrors().add(SponsorRowErrorDto.builder()
                            .rowNumber(r + 1)
                            .sponsorName(nome.trim())
                            .cnpj(cnpj.trim())
                            .errorMessage("Patrocinador já cadastrado na plataforma (CNPJ coincide com '" + existing.getName() + "').")
                            .build());
                    continue;
                }
                if (!cleanNameNorm.isBlank() && existingByName.containsKey(cleanNameNorm)) {
                    Sponsor existing = existingByName.get(cleanNameNorm);
                    errorCount++;
                    result.getErrors().add(SponsorRowErrorDto.builder()
                            .rowNumber(r + 1)
                            .sponsorName(nome.trim())
                            .cnpj(cnpj.trim())
                            .errorMessage("Patrocinador já cadastrado na plataforma (marca com nome '" + existing.getName() + "' já existe).")
                            .build());
                    continue;
                }

                // 2. Checagem contra a própria planilha (duplicidade interna)
                if (cleanCnpjDigits.length() >= 8 && seenSheetCnpjs.containsKey(cleanCnpjDigits)) {
                    int prevLine = seenSheetCnpjs.get(cleanCnpjDigits);
                    errorCount++;
                    result.getErrors().add(SponsorRowErrorDto.builder()
                            .rowNumber(r + 1)
                            .sponsorName(nome.trim())
                            .cnpj(cnpj.trim())
                            .errorMessage("Patrocinador duplicado na própria planilha (mesmo CNPJ da linha #" + prevLine + ").")
                            .build());
                    continue;
                }
                if (!cleanNameNorm.isBlank() && seenSheetNames.containsKey(cleanNameNorm)) {
                    int prevLine = seenSheetNames.get(cleanNameNorm);
                    errorCount++;
                    result.getErrors().add(SponsorRowErrorDto.builder()
                            .rowNumber(r + 1)
                            .sponsorName(nome.trim())
                            .cnpj(cnpj.trim())
                            .errorMessage("Patrocinador duplicado na própria planilha (mesmo Nome da linha #" + prevLine + ").")
                            .build());
                    continue;
                }

                // Registra nos mapas de controle
                if (cleanCnpjDigits.length() >= 8) seenSheetCnpjs.put(cleanCnpjDigits, r + 1);
                if (!cleanNameNorm.isBlank()) seenSheetNames.put(cleanNameNorm, r + 1);

                Sponsor sponsor = Sponsor.builder()
                        .name(nome.trim())
                        .cnpj(cnpj.trim())
                        .quota(quota)
                        .segment(segmento != null && !segmento.isBlank() ? segmento.trim() : null)
                        .contactName(contato != null && !contato.isBlank() ? contato.trim() : null)
                        .contactPhone(telefone != null && !telefone.isBlank() ? telefone.trim() : null)
                        .notes("Importado em lote via planilha pelo administrador")
                        .build();

                Sponsor saved = sponsorRepository.save(sponsor);
                if (cleanCnpjDigits.length() >= 8) existingByCnpj.put(cleanCnpjDigits, saved);
                if (!cleanNameNorm.isBlank()) existingByName.put(cleanNameNorm, saved);

                auditService.log(saved.getId(), actorEmail, "INSERT", "sponsors", "Cadastrou patrocinador em lote: " + saved.getName() + " (" + saved.getQuota().getLabel() + ")");
                result.getCreatedSponsors().add(toDto(saved));
                successCount++;
            }

            result.setTotalRows(totalRows);
            result.setSuccessCount(successCount);
            result.setErrorCount(errorCount);
            if (successCount == 0 && errorCount > 0) {
                result.setSuccess(false);
            }

        } catch (Exception e) {
            log.error("Erro ao importar planilha de patrocinadores", e);
            result.setSuccess(false);
            result.getErrors().add(SponsorRowErrorDto.builder()
                    .rowNumber(0)
                    .errorMessage("Falha ao processar arquivo Excel: " + e.getMessage())
                    .build());
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase().trim();
    }

    private String extractDigits(String input) {
        if (input == null) return "";
        return input.replaceAll("\\D", "");
    }

    private QuotaLevel parseQuotaLevel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = normalizeText(raw).replace(" ", "").replace("-", "").replace("_", "");
        if (clean.equals("master")) {
            return QuotaLevel.MASTER;
        } else if (clean.equals("subcotag") || clean.equals("subcotagrande")) {
            return QuotaLevel.SUBCOTA_G;
        } else if (clean.equals("subcota")) {
            return QuotaLevel.SUBCOTA;
        }
        return null;
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
