package com.filemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ---- En-tête du rapport ----
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String agencyName;


    /** Date à laquelle le rapport a été reçu / uploadé */
    @Column(nullable = false)
    private LocalDate receptionDate;

    /** Début de la plage de transactions */
    @Column(nullable = false)
    private LocalDate periodStart;

    /** Fin de la plage de transactions */
    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private String secretaryName;

    /** Montant total de toutes les transactions */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalVolume;

    /** Nombre de transactions dans le rapport */
    @Column(nullable = false)
    private Integer transactionCount;

    // ---- Lien vers le fichier source ----
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_document_id", nullable = false)
    private FileDocument fileDocument;

    // ---- Propriétaire ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- Transactions liées ----
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}
