package com.filemanager.service;

import com.filemanager.dto.DeleteResponse;
import com.filemanager.dto.FileResponse;
import com.filemanager.entity.FileDocument;
import com.filemanager.entity.User;
import com.filemanager.exception.FileNotFoundException;
import com.filemanager.exception.InvalidFileException;
import com.filemanager.repository.FileDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileDocumentRepository fileDocumentRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Dossier d'upload initialisé : {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload", e);
        }
    }

    /**
     * Upload d'un fichier PDF.
     * Valide le fichier, le stocke sur disque, enregistre les métadonnées en BDD.
     * Retourne un FileResponse aligné sur FileItem (frontend).
     */
    @Transactional
    public FileResponse uploadFile(MultipartFile file, User owner) {
        validateFile(file);

        String storedName = UUID.randomUUID() + ".pdf";
        Path destination = uploadPath.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }

        FileDocument doc = FileDocument.builder()
                .originalName(sanitizeFileName(file.getOriginalFilename()))
                .storedName(storedName)
                .filePath(destination.toString())
                .fileSize(file.getSize())
                .contentType("application/pdf")
                .owner(owner)
                .build();

        FileDocument saved = fileDocumentRepository.save(doc);
        log.info("Fichier uploadé : {} par {}", storedName, owner.getEmail());
        return toFileResponse(saved);
    }


    /**
     * Retourne uniquement les fichiers NON supprimés de l'utilisateur.
     * Correspond à fileService.getFiles(userId) côté frontend.
     */
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesForUser(User owner) {
        return fileDocumentRepository
                .findByOwnerAndDeletedFalseOrderByUploadedAtDesc(owner)
                .stream()
                .map(FileService::toFileResponse)
                .toList();
    }


    /**
     * Métadonnées d'un fichier visible (non supprimé).
     */
    @Transactional(readOnly = true)
    public FileResponse getFileInfo(String fileId, User owner) {
        return toFileResponse(getVisibleDocumentForUser(fileId, owner));
    }


    /**
     * Téléchargement d'un fichier.
     * Fonctionne même si le fichier est soft-deleted (la copie reste sur disque).
     * Correspond à fileService.download(fileId, userId) côté frontend.
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(String fileId, User owner) {
        // On cherche dans TOUS les fichiers (y compris supprimés) car la copie reste
        FileDocument doc = getAnyDocumentForUser(fileId, owner);

        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("Fichier introuvable sur le serveur");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Chemin de fichier invalide");
        }
    }

    /**
     * Retourne le nom original pour le header Content-Disposition.
     * Fonctionne aussi pour les fichiers soft-deleted.
     */
    @Transactional(readOnly = true)
    public String getOriginalName(String fileId, User owner) {
        return getAnyDocumentForUser(fileId, owner).getOriginalName();
    }


    /**
     * Suppression logique : marque le fichier comme supprimé en BDD.
     * Le fichier physique sur disque est CONSERVÉ.
     * L'entrée en BDD est CONSERVÉE (deleted = true, deletedAt = now).
     *
     * Correspond à fileService.delete(fileId, userId) côté frontend.
     * Retourne DeleteResponse { success, message } comme attendu.
     */
    @Transactional
    public DeleteResponse deleteFile(String fileId, User owner) {
        FileDocument doc = getVisibleDocumentForUser(fileId, owner);

        // Soft delete : on ne touche pas au fichier physique ni à l'entrée BDD
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        fileDocumentRepository.save(doc);

        log.info("Fichier soft-deleted : {} par {}", doc.getStoredName(), owner.getEmail());
        return new DeleteResponse(true, "Fichier supprimé avec succès");
    }


    /** Fichier visible (non supprimé) appartenant à l'utilisateur */
    private FileDocument getVisibleDocumentForUser(String fileId, User owner) {
        UUID uuid = parseUuid(fileId);
        return fileDocumentRepository.findByIdAndOwnerAndDeletedFalse(uuid, owner)
                .orElseThrow(() -> new FileNotFoundException(
                        "Fichier introuvable ou accès non autorisé"));
    }

    /** Tous les fichiers (y compris soft-deleted) appartenant à l'utilisateur */
    private FileDocument getAnyDocumentForUser(String fileId, User owner) {
        UUID uuid = parseUuid(fileId);
        return fileDocumentRepository.findByIdAndOwner(uuid, owner)
                .orElseThrow(() -> new FileNotFoundException(
                        "Fichier introuvable ou accès non autorisé"));
    }

    private UUID parseUuid(String fileId) {
        try {
            return UUID.fromString(fileId);
        } catch (IllegalArgumentException e) {
            throw new FileNotFoundException("Identifiant de fichier invalide");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Le fichier est vide");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) throw new InvalidFileException("Nom de fichier manquant");

        String nameLower = originalName.toLowerCase();

        if (nameLower.endsWith(".pdf")) {
            // Vérifie magic bytes %PDF-
            try {
                byte[] header = new byte[5];
                int read = file.getInputStream().read(header);
                if (read < 5 || !new String(header).equals("%PDF-")) {
                    throw new InvalidFileException("Contenu PDF invalide");
                }
            } catch (java.io.IOException e) {
                throw new InvalidFileException("Impossible de lire le fichier");
            }
        } else if (nameLower.endsWith(".csv")) {
            String contentType = file.getContentType();
            if (contentType != null &&
                    !contentType.contains("csv") &&
                    !contentType.contains("text") &&
                    !contentType.contains("octet-stream")) {
                throw new InvalidFileException("Type MIME CSV invalide : " + contentType);
            }
        } else {
            throw new InvalidFileException("Seuls les fichiers PDF et CSV sont acceptés");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "fichier.pdf";
        return fileName.replaceAll("[^a-zA-Z0-9._\\-àâäéèêëîïôùûü ]", "_");
    }

    /**
     * Convertit FileDocument → FileResponse.
     * Noms de champs alignés sur FileItem du frontend :
     *   name = originalName, size = fileSize, type = contentType, userId = owner.id
     */
    public static FileResponse toFileResponse(FileDocument doc) {
        return new FileResponse(
                doc.getId().toString(),
                doc.getOwner().getId().toString(),   // userId
                doc.getOriginalName(),               // name
                doc.getFileSize(),                   // size
                doc.getContentType(),                // type
                doc.getUploadedAt()                  // uploadedAt
        );
    }
}
