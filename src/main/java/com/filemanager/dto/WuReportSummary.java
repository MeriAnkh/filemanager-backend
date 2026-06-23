package com.filemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Résumé d'un rapport WU (liste)
 */
public record WuReportSummary(
        String id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate reportDate,
        String agentUsername,
        String terminalId,
        Integer paidCount,
        Integer sentCount,
        BigDecimal totalPaidAmountSent,
        BigDecimal totalPaidCollected,
        BigDecimal totalPaidFees,
        BigDecimal remainingBalance,
        String remainingBalanceCurrency,
        Boolean hasPaidTransactions,
        Boolean hasSentTransactions,
        String fileId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {
}
