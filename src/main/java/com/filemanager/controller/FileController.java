package com.filemanager.controller;

import com.filemanager.dto.DeleteResponse;
import com.filemanager.dto.FileResponse;
import com.filemanager.entity.User;
import com.filemanager.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * POST /api/files/upload
     * Correspond à fileService.upload(file, userId)
     * Retourne FileResponse aligné sur FileItem
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        FileResponse response = fileService.uploadFile(file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/files
     * Correspond à fileService.getFiles(userId)
     * Retourne uniquement les fichiers non supprimés
     */
    @GetMapping
    public ResponseEntity<List<FileResponse>> getMyFiles(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(fileService.getFilesForUser(currentUser));
    }

    /**
     * GET /api/files/{id}
     * Métadonnées d'un fichier visible
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileInfo(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(fileService.getFileInfo(id, currentUser));
    }

    /**
     * GET /api/files/{id}/download
     * Correspond à fileService.download(fileId, userId)
     * Fonctionne même si le fichier est soft-deleted (copie conservée sur disque)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        Resource resource = fileService.downloadFile(id, currentUser);
        String originalName = fileService.getOriginalName(id, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + originalName + "\""
                )
                .body(resource);
    }

    /**
     * DELETE /api/files/{id}
     * Correspond à fileService.delete(fileId, userId)
     * Soft delete : fichier marqué deleted=true en BDD, copie disque conservée
     * Retourne DeleteResponse { success, message } comme attendu par le frontend
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponse> deleteFile(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        DeleteResponse response = fileService.deleteFile(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
