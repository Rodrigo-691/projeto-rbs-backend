package com.rbs.planeta.service;

import com.rbs.planeta.dto.MediaImportResultDto;
import com.rbs.planeta.dto.MediaItemDto;
import com.rbs.planeta.model.MediaItem;
import com.rbs.planeta.model.Sponsor;
import com.rbs.planeta.repository.MediaItemRepository;
import com.rbs.planeta.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaItemRepository mediaRepository;
    private final SponsorRepository sponsorRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MediaItemDto> listBySponsor(UUID sponsorId) {
        return mediaRepository.findBySponsorIdOrderByDateDesc(sponsorId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MediaItemDto create(UUID sponsorId, MediaItemDto dto, String actorEmail) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Patrocinador não encontrado: " + sponsorId));

        MediaItem item = MediaItem.builder()
                .sponsor(sponsor)
                .title(dto.getTitle())
                .vehicle(dto.getVehicle())
                .date(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .reach(dto.getReach() != null ? dto.getReach() : 0L)
                .build();

        MediaItem saved = mediaRepository.save(item);
        auditService.log(sponsorId, actorEmail, "INSERT", "media_items", "Publicou veiculação: " + saved.getTitle() + " (" + saved.getVehicle() + ")");
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID sponsorId, UUID mediaId, String actorEmail) {
        MediaItem item = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Item de mídia não encontrado: " + mediaId));

        if (!item.getSponsor().getId().equals(sponsorId)) {
            throw new RuntimeException("Item de mídia não pertence ao patrocinador informado.");
        }

        mediaRepository.delete(item);
        auditService.log(sponsorId, actorEmail, "DELETE", "media_items", "Removeu veiculação: " + item.getTitle());
    }

    /**
     * Import multiple sponsor media reach entries from Excel file (.xlsx) using Apache POI.
     * Expected layout: Columns: [Data, Veículo, Sponsor 1, Sponsor 2, ...]
     */
    @Transactional
    public MediaImportResultDto importSpreadsheet(MultipartFile file, String actorEmail) {
        MediaImportResultDto result = new MediaImportResultDto();
        result.setSuccess(true);

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                result.setSuccess(false);
                result.getErrors().add("Planilha vazia ou sem dados suficientes.");
                return result;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.setSuccess(false);
                result.getErrors().add("Cabeçalho não encontrado.");
                return result;
            }

            // Build map of column index -> Sponsor
            Map<Integer, Sponsor> columnToSponsor = new HashMap<>();
            List<Sponsor> allSponsors = sponsorRepository.findAll();
            Map<String, Sponsor> sponsorNameMap = allSponsors.stream()
                    .collect(Collectors.toMap(s -> s.getName().trim().toLowerCase(), s -> s, (s1, s2) -> s1));

            int totalCols = headerRow.getLastCellNum();
            for (int c = 2; c < totalCols; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null) {
                    String sponsorName = cell.getStringCellValue().trim();
                    Sponsor match = sponsorNameMap.get(sponsorName.toLowerCase());
                    if (match != null) {
                        columnToSponsor.put(c, match);
                    } else {
                        result.getWarnings().add("Coluna " + (c + 1) + ": Patrocinador '" + sponsorName + "' não encontrado (coluna ignorada).");
                    }
                }
            }

            if (columnToSponsor.isEmpty()) {
                result.setSuccess(false);
                result.getErrors().add("Nenhum patrocinador cadastrado coincidiu com os cabeçalhos das colunas.");
                return result;
            }

            int rowsProcessed = 0;
            int itemsImported = 0;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell dateCell = row.getCell(0);
                Cell vehicleCell = row.getCell(1);

                if (dateCell == null || vehicleCell == null) continue;

                LocalDate date = parseDate(dateCell);
                String vehicle = vehicleCell.getStringCellValue().trim();

                if (vehicle.isBlank()) continue;
                rowsProcessed++;

                for (Map.Entry<Integer, Sponsor> entry : columnToSponsor.entrySet()) {
                    int colIdx = entry.getKey();
                    Sponsor sponsor = entry.getValue();

                    Cell reachCell = row.getCell(colIdx);
                    if (reachCell != null && reachCell.getCellType() == CellType.NUMERIC) {
                        long reach = (long) reachCell.getNumericCellValue();
                        if (reach > 0) {
                            MediaItem item = MediaItem.builder()
                                    .sponsor(sponsor)
                                    .title("Veiculação " + vehicle + " (" + date + ")")
                                    .vehicle(vehicle)
                                    .date(date)
                                    .reach(reach)
                                    .build();
                            mediaRepository.save(item);
                            itemsImported++;
                        }
                    }
                }
            }

            result.setRowsProcessed(rowsProcessed);
            result.setRowsImported(itemsImported);
            auditService.log(null, actorEmail, "INSERT", "media_items", "Importou planilha de mídia: " + itemsImported + " veiculações geradas");

        } catch (Exception e) {
            log.error("Erro ao importar planilha de mídia", e);
            result.setSuccess(false);
            result.getErrors().add("Erro ao ler arquivo Excel: " + e.getMessage());
        }

        return result;
    }

    private LocalDate parseDate(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return LocalDate.parse(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private MediaItemDto toDto(MediaItem item) {
        return MediaItemDto.builder()
                .id(item.getId())
                .sponsorId(item.getSponsor().getId())
                .title(item.getTitle())
                .vehicle(item.getVehicle())
                .date(item.getDate())
                .reach(item.getReach())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
