package com.filemanager.repository;

import com.filemanager.entity.FileDocument;
import com.filemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileDocumentRepository extends JpaRepository<FileDocument, UUID> {

    /** Fichiers visibles de l'utilisateur (non supprimés), triés par date desc */
    List<FileDocument> findByOwnerAndDeletedFalseOrderByUploadedAtDesc(User owner);

    /** Cherche un fichier visible par id + propriétaire */
    Optional<FileDocument> findByIdAndOwnerAndDeletedFalse(UUID id, User owner);

    /**
     * Cherche TOUS les fichiers (y compris supprimés) — usage admin ou download
     * Un fichier soft-deleted reste téléchargeable si l'on a son id direct
     */
    Optional<FileDocument> findByIdAndOwner(UUID id, User owner);

    boolean existsByIdAndOwner(UUID id, User owner);
}
