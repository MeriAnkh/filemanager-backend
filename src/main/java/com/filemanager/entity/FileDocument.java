package com.filemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Nom original du fichier  */
    @Column(nullable = false)
    private String originalName;

    /** Nom unique sur le disque (UUID + .pdf) pour éviter les collisions */
    @Column(nullable = false, unique = true)
    private String storedName;

    /** Chemin absolu sur le serveur */
    @Column(nullable = false)
    private String filePath;

    /** Taille en octets */
    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private String contentType = "application/pdf";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    /**
     * Soft delete : true = supprimé côté utilisateur.
     * Le fichier reste en BDD et sur disque.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /** Date à laquelle l'utilisateur a "supprimé" le fichier */
    @Column
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
