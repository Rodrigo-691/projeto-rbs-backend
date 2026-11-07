package com.rbs.planeta.dto;

import com.rbs.planeta.model.QuotaLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorDto {

    private UUID id;

    @NotBlank(message = "O nome da marca é obrigatório")
    private String name;

    @NotNull(message = "A cota é obrigatória")
    private QuotaLevel quota;

    private String segment;
    private String cnpj;
    private String contactName;
    private String contactPhone;
    private String notes;
    private OffsetDateTime createdAt;

    // Aggregates for executive overview
    private long membersCount;
    private long filesCount;
    private long mediaCount;
    private Long totalReach;
}
