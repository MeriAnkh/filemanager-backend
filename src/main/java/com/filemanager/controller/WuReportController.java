package com.filemanager.controller;

import com.filemanager.dto.WuReportDetail;
import com.filemanager.dto.WuReportSummary;
import com.filemanager.entity.User;
import com.filemanager.service.WuReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wu-reports")
@RequiredArgsConstructor
public class WuReportController {

    private final WuReportService wuReportService;

    /**
     * POST /api/wu-reports/parse/{fileId}
     * Parse un CSV WU déjà uploadé et sauvegarde le rapport.
     */
    @PostMapping("/parse/{fileId}")
    public ResponseEntity<WuReportSummary> parseFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(wuReportService.parseAndSave(fileId, currentUser));
    }

    /**
     * GET /api/wu-reports
     * Liste tous les rapports WU de l'utilisateur connecté.
     */
    @GetMapping
    public ResponseEntity<List<WuReportSummary>> getMyReports(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(wuReportService.getReportsForUser(currentUser));
    }

    /**
     * GET /api/wu-reports/{id}
     * Détail complet : en-tête + transactions payées + transactions envoyées.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WuReportDetail> getReportDetail(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(wuReportService.getReportDetail(id, currentUser));
    }

    /**
     * DELETE /api/wu-reports/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        wuReportService.deleteReport(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
