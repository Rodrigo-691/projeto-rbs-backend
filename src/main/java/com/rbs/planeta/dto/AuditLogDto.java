package com.rbs.planeta.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {

    private Long id;
    private UUID sponsorId;
    private String sponsorName;
    private String actorEmail;
    private String action;
    private String summary;
    private String tableName;
    private OffsetDateTime createdAt;
}
