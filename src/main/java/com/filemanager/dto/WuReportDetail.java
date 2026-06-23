package com.filemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail complet avec transactions
 */
public record WuReportDetail(
        String id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate reportDate,
        String agentUsername,
        String terminalId,
        Integer paidCount,
        Integer sentCount,
        BigDecimal totalPaidAmountSent,
        BigDecimal totalPaidTransferFees,
        BigDecimal totalPaidCollected,
        BigDecimal totalPaidExpected,
        BigDecimal totalPaidFees,
        BigDecimal totalPaidTaxes,
        BigDecimal remainingBalance,
        String remainingBalanceCurrency,
        Boolean hasPaidTransactions,
        Boolean hasSentTransactions,
        Boolean hasAnomalies,
        Boolean hasModifiedTransactions,
        Boolean hasRefunds,
        Boolean hasCancellations,
        String fileId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        List<WuTransactionDTO> paidTransactions,
        List<WuTransactionDTO> sentTransactions
) {
}
