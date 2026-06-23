package com.filemanager.service;

import com.filemanager.entity.WuReport;
import com.filemanager.entity.WuTransaction;
import com.filemanager.entity.WuTransaction.TransactionNature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parser du CSV Western Union.
 *
 * Structure du fichier :
 *   "Titre de section","date-début","date-fin"
 *   ""                                          ← ligne vide
 *   "Aucune transaction..." OU en-têtes colonnes
 *   lignes de données...
 *   ""                                          ← ligne vide = fin de section
 *   ...autres sections...
 *   "","","Solde restant","XAF -586 989"        ← ligne finale
 */
@Component
@Slf4j
public class CsvReportParser {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter TIME_FMT_12 =
            DateTimeFormatter.ofPattern("h:mm a");

    // Titres de sections reconnus
    private static final String SEC_SENT        = "Transactions envoyées";
    private static final String SEC_PAID        = "Transactions Payées";
    private static final String SEC_ANOMALIES   = "Liste des anomalies";
    private static final String SEC_MODIFIED    = "TRANSACCIONES MODIFICADAS";
    private static final String SEC_REFUND      = "Rapport de remboursement";
    private static final String SEC_CANCEL      = "Annuler rapport";
    private static final String SEC_UNPAID      = "Reporte sobre los no pagados";
    private static final String SEC_PENDING     = "Garder en suspens les transactions";

    // ---- Résultat du parsing ----
    public record ParsedWuReport(
            WuReport report,
            List<WuTransaction> transactions
    ) {}

    public ParsedWuReport parse(File csvFile) throws IOException {
        List<String[]> allLines = readAllLines(csvFile);

        WuReport.WuReportBuilder builder = WuReport.builder();
        List<WuTransaction> transactions  = new ArrayList<>();

        LocalDate reportDate  = LocalDate.now();
        String terminalId     = null;
        String agentUsername  = null;

        // Totaux payées (extraits de la ligne "Totale")
        BigDecimal totalAmountSent   = BigDecimal.ZERO;
        BigDecimal totalTransferFees = BigDecimal.ZERO;
        BigDecimal totalCollected    = BigDecimal.ZERO;
        BigDecimal totalExpected     = BigDecimal.ZERO;
        BigDecimal totalFees         = BigDecimal.ZERO;
        BigDecimal totalTaxes        = BigDecimal.ZERO;

        // Solde restant
        BigDecimal remainingBalance  = BigDecimal.ZERO;
        String remainingCurrency     = "XAF";

        // Flags sections
        boolean hasSent = false, hasPaid = false, hasAnomalies = false;
        boolean hasModified = false, hasRefunds = false, hasCancels = false;
        boolean hasUnpaid = false, hasPending = false;

        // Parsing ligne par ligne
        String currentSection  = null;
        boolean inDataBlock    = false; // on a passé la ligne d'en-têtes colonnes
        TransactionNature currentNature = null;

        for (int i = 0; i < allLines.size(); i++) {
            String[] cols = allLines.get(i);
            if (cols.length == 0) continue;

            String first = clean(cols[0]);
            if (first.isEmpty() && allLines.get(i).length <= 1) {
                // ligne vide → reset du bloc de données
                inDataBlock = false;
                continue;
            }

            // ---- Détection d'une ligne de section ----
            if (isSectionHeader(first)) {
                currentSection = first;
                inDataBlock    = false;
                currentNature  = null;

                // Date du rapport (colonne 1 ou 2)
                if (cols.length >= 2 && !clean(cols[1]).isEmpty()) {
                    reportDate = parseDate(clean(cols[1]));
                }

                // Flags
                switch (first) {
                    case SEC_SENT     -> { hasSent     = true; currentNature = TransactionNature.SENT; }
                    case SEC_PAID     -> { hasPaid     = true; currentNature = TransactionNature.PAID; }
                    case SEC_ANOMALIES-> hasAnomalies  = true;
                    case SEC_MODIFIED -> hasModified   = true;
                    case SEC_REFUND   -> hasRefunds    = true;
                    case SEC_CANCEL   -> hasCancels    = true;
                    case SEC_UNPAID   -> hasUnpaid     = true;
                    case SEC_PENDING  -> hasPending    = true;
                }
                continue;
            }

            // ---- Message "Aucune transaction..." → section vide ----
            if (first.startsWith("Aucun") || first.startsWith("Ninguna")) {
                if (currentSection != null) {
                    switch (currentSection) {
                        case SEC_SENT  -> hasSent  = false;
                        case SEC_PAID  -> hasPaid  = false;
                    }
                }
                continue;
            }

            // ---- Ligne d'en-têtes colonnes (MTCN, Receveur, etc.) ----
            if (isTransactionHeader(first)) {
                inDataBlock = true;
                continue;
            }

            // ---- Ligne "Totale" ----
            if (isTotal(cols)) {
                // Structure : "","","","","","","","","","","","","Totale",amountSent,fees,...
                if (cols.length >= 24) {
                    totalAmountSent   = parseBD(cols[13]);
                    totalTransferFees = parseBD(cols[14]);
                    totalCollected    = parseBD(cols[19]);
                    totalExpected     = parseBD(cols[21]);
                    totalFees         = parseBD(cols[22]);
                    totalTaxes        = parseBD(cols[23]);
                }
                inDataBlock = false;
                continue;
            }

            // ---- Ligne "Solde restant" ----
            if (isSoldeRestant(cols)) {
                // "","","","","","","","","","","","","","","","","","","","","","Solde restant","XAF -586 989"
                String soldeRaw = cols.length >= 23 ? clean(cols[22]) : "";
                if (!soldeRaw.isEmpty()) {
                    String[] parts = soldeRaw.split("\\s+", 2);
                    if (parts.length == 2) {
                        remainingCurrency = parts[0];
                        remainingBalance  = parseBD(parts[1].replace(" ", ""));
                    }
                }
                continue;
            }

            // ---- Ligne de transaction ----
            if (inDataBlock && currentNature != null && cols.length >= 25) {
                WuTransaction tx = parseTransaction(cols, currentNature);
                transactions.add(tx);

                // Récupère terminalId et agentUsername depuis la première transaction
                if (terminalId == null)    terminalId    = clean(cols[2]);
                if (agentUsername == null) agentUsername = clean(cols[5]);
            }
        }

        // Compteurs par nature
        long paidCount = transactions.stream()
                .filter(t -> t.getNature() == TransactionNature.PAID).count();
        long sentCount = transactions.stream()
                .filter(t -> t.getNature() == TransactionNature.SENT).count();

        WuReport report = builder
                .reportDate(reportDate)
                .terminalId(terminalId)
                .agentUsername(agentUsername)
                .totalPaidAmountSent(totalAmountSent)
                .totalPaidTransferFees(totalTransferFees)
                .totalPaidCollected(totalCollected)
                .totalPaidExpected(totalExpected)
                .totalPaidFees(totalFees)
                .totalPaidTaxes(totalTaxes)
                .paidCount((int) paidCount)
                .sentCount((int) sentCount)
                .remainingBalance(remainingBalance)
                .remainingBalanceCurrency(remainingCurrency)
                .hasSentTransactions(sentCount > 0)
                .hasPaidTransactions(paidCount > 0)
                .hasAnomalies(hasAnomalies)
                .hasModifiedTransactions(hasModified)
                .hasRefunds(hasRefunds)
                .hasCancellations(hasCancels)
                .hasUnpaid(hasUnpaid)
                .hasPending(hasPending)
                .build();

        log.info("CSV WU parsé : {} tx payées, {} tx envoyées, solde {}{}",
                paidCount, sentCount, remainingCurrency, remainingBalance);

        return new ParsedWuReport(report, transactions);
    }

    // ================================================================
    //  Parse une ligne de transaction (25 colonnes)
    // ================================================================
    private WuTransaction parseTransaction(String[] c, TransactionNature nature) {
        return WuTransaction.builder()
                .nature(nature)
                .countryOriginCode(clean(c[0]))
                .currencyOriginCode(clean(c[1]))
                .terminalId(clean(c[2]))
                .operatorId(clean(c[3]))
                .supervisorId(clean(c[4]))
                .username(clean(c[5]))
                .mtcn(clean(c[6]))
                .receiver(clean(c[7]))
                .sender(clean(c[8]))
                .countryDestCode(clean(c[9]))
                .currencyDestCode(clean(c[10]))
                .transactionType(clean(c[11]))
                .date(parseDate(clean(c[12])))
                .time(parseTime(clean(c[13])))
                .amountSent(parseBD(c[14]))
                .transferFee(parseBD(c[15]))
                .deliveryFee(parseBD(c[16]))
                .messageFee(parseBD(c[17]))
                .promoDiscount(parseBD(c[18]))
                .totalCollected(parseBD(c[19]))
                .exchangeRate(parseBD(c[20]))
                .expectedAmountPaid(parseBD(c[21]))
                .totalFees(parseBD(c[22]))
                .totalTaxes(parseBD(c[23]))
                .paymentType(c.length > 24 ? clean(c[24]) : null)
                .build();
    }

    // ================================================================
    //  Détection des types de lignes
    // ================================================================
    private boolean isSectionHeader(String first) {
        return List.of(SEC_SENT, SEC_PAID, SEC_ANOMALIES, SEC_MODIFIED,
                        SEC_REFUND, SEC_CANCEL, SEC_UNPAID, SEC_PENDING,
                        "Rapport quotidien de l'agent de carte de débit",
                        "Rapport de la carte de débit")
                .stream().anyMatch(s -> s.equalsIgnoreCase(first));
    }

    private boolean isTransactionHeader(String first) {
        // La ligne d'en-têtes contient "Code du Pays d'Origine" en première colonne
        return first.equalsIgnoreCase("Code du Pays d'Origine")
                || first.equalsIgnoreCase("Código País Origen");
    }

    private boolean isTotal(String[] cols) {
        // La ligne Totale a "Totale" à la position 12 (index 12)
        return cols.length >= 13 && clean(cols[12]).equalsIgnoreCase("Totale");
    }

    private boolean isSoldeRestant(String[] cols) {
        // La dernière ligne a "Solde restant" vers la fin
        return Arrays.stream(cols).anyMatch(c ->
                clean(c).equalsIgnoreCase("Solde restant"));
    }

    // ================================================================
    //  Lecture CSV avec guillemets
    // ================================================================
    private List<String[]> readAllLines(File file) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(parseCsvLine(line));
            }
        }
        return result;
    }

    /**
     * Parse une ligne CSV avec guillemets simples.
     * "FR","EUR","A8XU" → ["FR","EUR","A8XU"]
     */
    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    // ================================================================
    //  Utilitaires
    // ================================================================
    private String clean(String s) {
        return (s == null) ? "" : s.trim();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(raw.trim(), DATE_FMT); }
        catch (Exception e) {
            log.warn("Date non parsée : '{}'", raw);
            return LocalDate.now();
        }
    }

    private LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // "10:54 AM" → LocalTime
            return LocalTime.parse(raw.trim().toUpperCase(), TIME_FMT_12);
        } catch (Exception e) {
            log.warn("Heure non parsée : '{}'", raw);
            return null;
        }
    }

    private BigDecimal parseBD(String raw) {
        if (raw == null || raw.isBlank() || clean(raw).equalsIgnoreCase("null")) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim().replace(" ", "").replace(",", "."));
        } catch (Exception e) {
            log.warn("Montant non parsé : '{}'", raw);
            return BigDecimal.ZERO;
        }
    }
}
