package com.filemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Résumé d'un rapport (liste)
 */
public record ReportSummary(
        String id,
        String title,
        String agencyName,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate receptionDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd,
        String secretaryName,
        BigDecimal totalVolume,
        Integer transactionCount,
        String fileId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
}
