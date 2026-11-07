package com.rbs.planeta.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaImportResultDto {

    private boolean success;
    private int rowsProcessed;
    private int rowsImported;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
