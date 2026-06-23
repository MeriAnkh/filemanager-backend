package com.filemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * En-tête d'un rapport Western Union (CSV).
 * Une ligne par fichier CSV importé.
 */
@Entity
@Table(name = "wu_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WuReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Date du rapport (extraite du CSV, ex: 16-06-2026) */
    @Column(nullable = false)
    private LocalDate reportDate;

    /** Nom de l'agent / agence (extrait du username des transactions) */
    private String agentUsername;

    /** Terminal identifiant (ex: A8XU) */
    private String terminalId;

    // ---- Totaux des transactions payées ----
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidAmountSent   = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidTransferFees = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidCollected    = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidExpected     = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidFees         = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidTaxes        = BigDecimal.ZERO;

    /** Nombre de transactions payées */
    @Builder.Default
    private Integer paidCount = 0;

    /** Nombre de transactions envoyées */
    @Builder.Default
    private Integer sentCount = 0;

    /** Solde restant (dernière ligne du CSV) */
    @Column(precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    private String remainingBalanceCurrency;

    // ---- Sections sans transactions (messages "Aucune...") ----
    @Builder.Default
    private Boolean hasSentTransactions      = false;
    @Builder.Default
    private Boolean hasPaidTransactions      = false;
    @Builder.Default
    private Boolean hasAnomalies             = false;
    @Builder.Default
    private Boolean hasModifiedTransactions  = false;
    @Builder.Default
    private Boolean hasRefunds               = false;
    @Builder.Default
    private Boolean hasCancellations         = false;
    @Builder.Default
    private Boolean hasUnpaid                = false;
    @Builder.Default
    private Boolean hasPending               = false;

    /** Lien vers le fichier CSV source */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_document_id")
    private FileDocument fileDocument;

    /** Propriétaire */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Toutes les transactions (envoyées + payées) */
    @OneToMany(mappedBy = "wuReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WuTransaction> transactions = new ArrayList<>();
}
