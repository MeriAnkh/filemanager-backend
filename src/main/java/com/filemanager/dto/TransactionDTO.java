package com.filemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Une ligne de transaction */
public record TransactionDTO(
        String id,
        String reference,
        String country,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        String operator,
        BigDecimal amount,
        String currency,
        String service
) {}
