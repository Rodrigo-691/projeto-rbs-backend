package com.rbs.planeta.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorRowErrorDto {
    private int rowNumber;
    private String sponsorName;
    private String cnpj;
    private String errorMessage;
}
