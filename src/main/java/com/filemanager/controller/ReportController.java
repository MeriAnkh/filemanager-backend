package com.filemanager.controller;

import com.filemanager.dto.ReportDetail;
import com.filemanager.dto.ReportSummary;
import com.filemanager.entity.User;
import com.filemanager.service.ReportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportPdfService reportPdfService;

    /**
     * POST /api/reports/parse/{fileId}
     * Lance le parsing d'un fichier déjà uploadé et sauvegarde le rapport.
     * L'utilisateur uploade d'abord via /api/files/upload,
     * puis appelle cet endpoint avec l'id retourné.
     */
    @PostMapping("/parse/{fileId}")
    public ResponseEntity<ReportSummary> parseFile(

            @PathVariable String fileId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportPdfService.parseAndSave(fileId, currentUser));
    }

    /**
     * GET /api/reports
     * Liste tous les rapports de l'utilisateur connecté.
     */
    @GetMapping
    public ResponseEntity<List<ReportSummary>> getMyReports(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportPdfService.getReportsForUser(currentUser));
    }


    /**
     * GET /api/reports/{id}
     * Détail complet : en-tête + tableau des transactions.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportDetail> getReportDetail(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportPdfService.getReportDetail(id, currentUser));
    }

    /**
     * DELETE /api/reports/{id}
     * Supprime le rapport et ses transactions (le fichier source reste intact).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        reportPdfService.deleteReport(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
