package com.rbs.planeta.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorBatchImportResultDto {
    private boolean success;
    private int totalRows;
    private int successCount;
    private int errorCount;

    @Builder.Default
    private List<SponsorDto> createdSponsors = new ArrayList<>();

    @Builder.Default
    private List<SponsorRowErrorDto> errors = new ArrayList<>();
}
