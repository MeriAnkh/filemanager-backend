package com.filemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Une transaction WU */
public record WuTransactionDTO(
        String id,
        String nature,
        String countryOriginCode,
        String currencyOriginCode,
        String terminalId,
        String operatorId,
        String username,
        String mtcn,
        String receiver,
        String sender,
        String countryDestCode,
        String currencyDestCode,
        String transactionType,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        BigDecimal amountSent,
        BigDecimal transferFee,
        BigDecimal deliveryFee,
        BigDecimal messageFee,
        BigDecimal promoDiscount,
        BigDecimal totalCollected,
        BigDecimal exchangeRate,
        BigDecimal expectedAmountPaid,
        BigDecimal totalFees,
        BigDecimal totalTaxes,
        String paymentType
) {}
