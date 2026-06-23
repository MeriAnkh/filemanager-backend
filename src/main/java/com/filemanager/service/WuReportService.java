package com.filemanager.service;

import com.filemanager.dto.WuReportDetail;
import com.filemanager.dto.WuReportSummary;
import com.filemanager.dto.WuTransactionDTO;
import com.filemanager.entity.FileDocument;
import com.filemanager.entity.User;
import com.filemanager.entity.WuReport;
import com.filemanager.entity.WuTransaction;
import com.filemanager.entity.WuTransaction.TransactionNature;
import com.filemanager.exception.FileNotFoundException;
import com.filemanager.repository.FileDocumentRepository;
import com.filemanager.repository.WuReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WuReportService {

    private final WuReportRepository    wuReportRepository;
    private final FileDocumentRepository fileDocumentRepository;
    private final CsvReportParser        csvParser;

    // ----------------------------------------------------------------
    //  Parse + sauvegarde
    // ----------------------------------------------------------------
    @Transactional
    public WuReportSummary parseAndSave(String fileDocumentId, User owner) {
        UUID fileId = UUID.fromString(fileDocumentId);

        FileDocument fileDoc = fileDocumentRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new FileNotFoundException("Fichier introuvable"));

        if (!fileDoc.getOriginalName().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Ce service ne traite que les CSV Western Union");
        }

        CsvReportParser.ParsedWuReport parsed;
        try {
            parsed = csvParser.parse(new File(fileDoc.getFilePath()));
        } catch (Exception e) {
            log.error("Erreur parsing CSV WU : {}", e.getMessage());
            throw new RuntimeException("Impossible de lire le fichier CSV : " + e.getMessage());
        }

        WuReport report = parsed.report();
        report.setFileDocument(fileDoc);
        report.setOwner(owner);

        List<WuTransaction> transactions = parsed.transactions();
        transactions.forEach(tx -> tx.setWuReport(report));
        report.getTransactions().addAll(transactions);

        WuReport saved = wuReportRepository.save(report);
        log.info("Rapport WU {} sauvegardé : {} tx payées, {} tx envoyées",
                saved.getReportDate(), saved.getPaidCount(), saved.getSentCount());

        return toSummary(saved);
    }

    // ----------------------------------------------------------------
    //  Lecture
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<WuReportSummary> getReportsForUser(User owner) {
        return wuReportRepository.findByOwnerOrderByReportDateDesc(owner)
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public WuReportDetail getReportDetail(String reportId, User owner) {
        UUID id = UUID.fromString(reportId);
        WuReport report = wuReportRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new FileNotFoundException("Rapport WU introuvable"));
        return toDetail(report);
    }

    // ----------------------------------------------------------------
    //  Suppression
    // ----------------------------------------------------------------
    @Transactional
    public void deleteReport(String reportId, User owner) {
        UUID id = UUID.fromString(reportId);
        WuReport report = wuReportRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new FileNotFoundException("Rapport WU introuvable"));
        wuReportRepository.delete(report);
    }

    // ----------------------------------------------------------------
    //  Mappers
    // ----------------------------------------------------------------
    private WuReportSummary toSummary(WuReport r) {
        return new WuReportSummary(
                r.getId().toString(),
                r.getReportDate(),
                r.getAgentUsername(),
                r.getTerminalId(),
                r.getPaidCount(),
                r.getSentCount(),
                r.getTotalPaidAmountSent(),
                r.getTotalPaidCollected(),
                r.getTotalPaidFees(),
                r.getRemainingBalance(),
                r.getRemainingBalanceCurrency(),
                r.getHasPaidTransactions(),
                r.getHasSentTransactions(),
                r.getFileDocument() != null ? r.getFileDocument().getId().toString() : null,
                r.getCreatedAt()
        );
    }

    private WuReportDetail toDetail(WuReport r) {
        List<WuTransactionDTO> paid = r.getTransactions().stream()
                .filter(t -> t.getNature() == TransactionNature.PAID)
                .sorted((a, b) -> {
                    int d = a.getDate().compareTo(b.getDate());
                    if (d != 0) return d;
                    if (a.getTime() == null || b.getTime() == null) return 0;
                    return a.getTime().compareTo(b.getTime());
                })
                .map(this::toTxDto).toList();

        List<WuTransactionDTO> sent = r.getTransactions().stream()
                .filter(t -> t.getNature() == TransactionNature.SENT)
                .sorted((a, b) -> {
                    int d = a.getDate().compareTo(b.getDate());
                    if (d != 0) return d;
                    if (a.getTime() == null || b.getTime() == null) return 0;
                    return a.getTime().compareTo(b.getTime());
                })
                .map(this::toTxDto).toList();

        return new WuReportDetail(
                r.getId().toString(),
                r.getReportDate(),
                r.getAgentUsername(),
                r.getTerminalId(),
                r.getPaidCount(),
                r.getSentCount(),
                r.getTotalPaidAmountSent(),
                r.getTotalPaidTransferFees(),
                r.getTotalPaidCollected(),
                r.getTotalPaidExpected(),
                r.getTotalPaidFees(),
                r.getTotalPaidTaxes(),
                r.getRemainingBalance(),
                r.getRemainingBalanceCurrency(),
                r.getHasPaidTransactions(),
                r.getHasSentTransactions(),
                r.getHasAnomalies(),
                r.getHasModifiedTransactions(),
                r.getHasRefunds(),
                r.getHasCancellations(),
                r.getFileDocument() != null ? r.getFileDocument().getId().toString() : null,
                r.getCreatedAt(),
                paid,
                sent
        );
    }

    private WuTransactionDTO toTxDto(WuTransaction t) {
        return new WuTransactionDTO(
                t.getId().toString(),
                t.getNature().name(),
                t.getCountryOriginCode(),
                t.getCurrencyOriginCode(),
                t.getTerminalId(),
                t.getOperatorId(),
                t.getUsername(),
                t.getMtcn(),
                t.getReceiver(),
                t.getSender(),
                t.getCountryDestCode(),
                t.getCurrencyDestCode(),
                t.getTransactionType(),
                t.getDate(),
                t.getTime(),
                t.getAmountSent(),
                t.getTransferFee(),
                t.getDeliveryFee(),
                t.getMessageFee(),
                t.getPromoDiscount(),
                t.getTotalCollected(),
                t.getExchangeRate(),
                t.getExpectedAmountPaid(),
                t.getTotalFees(),
                t.getTotalTaxes(),
                t.getPaymentType()
        );
    }
}
