package com.filemanager.service;

import com.filemanager.dto.ReportDetail;
import com.filemanager.dto.ReportSummary;
import com.filemanager.dto.TransactionDTO;
import com.filemanager.entity.FileDocument;
import com.filemanager.entity.Report;
import com.filemanager.entity.Transaction;
import com.filemanager.entity.User;
import com.filemanager.exception.FileNotFoundException;
import com.filemanager.repository.FileDocumentRepository;
import com.filemanager.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Gère UNIQUEMENT les rapports PDF (RIA, Money Gram, etc.)
 * Les rapports CSV Western Union sont gérés par WuReportService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfService {

    private final ReportRepository       reportRepository;
    private final FileDocumentRepository fileDocumentRepository;
    private final PdfReportParser        pdfParser;  // ← uniquement le parser PDF

    // ----------------------------------------------------------------
    //  Parse + sauvegarde (PDF uniquement)
    // ----------------------------------------------------------------
    @Transactional
    public ReportSummary parseAndSave(String fileDocumentId, User owner) {
        UUID fileId = UUID.fromString(fileDocumentId);

        FileDocument fileDoc = fileDocumentRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new FileNotFoundException("Fichier introuvable"));

        String name = fileDoc.getOriginalName().toLowerCase();

        if (!name.endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "ReportService ne traite que les PDF. Pour les CSV, utilisez WuReportService."
            );
        }

        PdfReportParser.ParsedReport parsed;
        try {
            parsed = pdfParser.parse(new File(fileDoc.getFilePath()));
        } catch (Exception e) {
            log.error("Erreur parsing PDF : {}", e.getMessage());
            throw new RuntimeException("Impossible de lire le fichier PDF : " + e.getMessage());
        }

        Report report = parsed.report();
        report.setFileDocument(fileDoc);
        report.setOwner(owner);

        List<Transaction> transactions = parsed.transactions();
        transactions.forEach(tx -> tx.setReport(report));
        report.getTransactions().addAll(transactions);

        if (report.getTransactionCount() == null || report.getTransactionCount() == 0) {
            report.setTransactionCount(transactions.size());
        }

        Report saved = reportRepository.save(report);
        log.info("Rapport PDF '{}' sauvegardé avec {} transactions",
                saved.getTitle(), transactions.size());
        return toSummary(saved);
    }

    // ----------------------------------------------------------------
    //  Lecture
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ReportSummary> getReportsForUser(User owner) {
        return reportRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ReportDetail getReportDetail(String reportId, User owner) {
        UUID id = UUID.fromString(reportId);
        Report report = reportRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new FileNotFoundException("Rapport introuvable"));
        return toDetail(report);
    }

    // ----------------------------------------------------------------
    //  Suppression
    // ----------------------------------------------------------------
    @Transactional
    public void deleteReport(String reportId, User owner) {
        UUID id = UUID.fromString(reportId);
        Report report = reportRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new FileNotFoundException("Rapport introuvable"));
        reportRepository.delete(report);
        log.info("Rapport PDF {} supprimé", reportId);
    }

    // ----------------------------------------------------------------
    //  Mappers
    // ----------------------------------------------------------------
    private ReportSummary toSummary(Report r) {
        return new ReportSummary(
                r.getId().toString(),
                r.getTitle(),
                r.getAgencyName(),
                r.getReceptionDate(),
                r.getPeriodStart(),
                r.getPeriodEnd(),
                r.getSecretaryName(),
                r.getTotalVolume(),
                r.getTransactionCount(),
                r.getFileDocument().getId().toString(),
                r.getCreatedAt()
        );
    }

    private ReportDetail toDetail(Report r) {
        List<TransactionDTO> txDtos = r.getTransactions().stream()
                .map(tx -> new TransactionDTO(
                        tx.getId().toString(),
                        tx.getReference(),
                        tx.getCountry(),
                        tx.getDate(),
                        tx.getOperator(),
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getService()
                ))
                .toList();

        return new ReportDetail(
                r.getId().toString(),
                r.getTitle(),
                r.getAgencyName(),
                r.getReceptionDate(),
                r.getPeriodStart(),
                r.getPeriodEnd(),
                r.getSecretaryName(),
                r.getTotalVolume(),
                r.getTransactionCount(),
                r.getFileDocument().getId().toString(),
                r.getCreatedAt(),
                txDtos
        );
    }
}