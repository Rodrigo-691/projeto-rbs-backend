package com.rbs.planeta.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedUrlResponse {

    private String url;
    private String path;
    private OffsetDateTime expiresAt;
}
