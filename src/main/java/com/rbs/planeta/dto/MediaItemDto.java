package com.rbs.planeta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaItemDto {

    private UUID id;
    private UUID sponsorId;

    @NotBlank(message = "O título é obrigatório")
    private String title;

    @NotBlank(message = "O veículo é obrigatório (TV, Rádio, Digital ou Jornal)")
    private String vehicle;

    @NotNull(message = "A data é obrigatória")
    private LocalDate date;

    private Long reach;
    private OffsetDateTime createdAt;
}
