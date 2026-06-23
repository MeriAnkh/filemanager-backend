package com.filemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Une transaction Western Union (envoyée ou payée).
 * Liée à un WuReport par clé étrangère.
 */
@Entity
@Table(name = "wu_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WuTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** SENT ou PAID */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionNature nature;

    // ---- Champs communs ----
    private String countryOriginCode;
    private String currencyOriginCode;
    private String terminalId;
    private String operatorId;
    private String supervisorId;
    private String username;
    private String mtcn;               // référence WU
    private String receiver;
    private String sender;
    private String countryDestCode;
    private String currencyDestCode;
    private String transactionType;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime time;

    // ---- Montants ----
    @Column(precision = 15, scale = 2)
    private BigDecimal amountSent;

    @Column(precision = 15, scale = 2)
    private BigDecimal transferFee;

    @Column(precision = 15, scale = 2)
    private BigDecimal deliveryFee;

    @Column(precision = 15, scale = 2)
    private BigDecimal messageFee;

    @Column(precision = 15, scale = 2)
    private BigDecimal promoDiscount;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalCollected;

    @Column(precision = 15, scale = 4)
    private BigDecimal exchangeRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal expectedAmountPaid;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalFees;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalTaxes;

    private String paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wu_report_id", nullable = false)
    private WuReport wuReport;

    public enum TransactionNature {
        SENT, PAID
    }
}
