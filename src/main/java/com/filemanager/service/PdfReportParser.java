package com.filemanager.service;

import com.filemanager.entity.Report;
import com.filemanager.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PdfReportParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    /**
     * Exemple :
     * Rapport Agence ETS LA GAIETE MVOG-ADA pour le produit RIA Du 16/06/26 Au 16/06/26
     */
    private static final Pattern HEADER_PATTERN =
            Pattern.compile(
                    "(Rapport\\s+Agence\\s+.+?\\s+pour\\s+le\\s+produit\\s+.+?)\\s+Du\\s+(\\d{2}/\\d{2}/\\d{2})\\s+Au\\s+(\\d{2}/\\d{2}/\\d{2})",
                    Pattern.CASE_INSENSITIVE
            );

    /**
         * WAFACASH : DJIKEU TANKEU FRANCELISE
     */
    private static final Pattern SECRETARY_PATTERN =
            Pattern.compile(
                    "WAFACASH\\s*:\\s*([^\\n\\r]+?)(?=\\s*Edit[ée]\\s*le|\\r|\\n|$)",
                    Pattern.CASE_INSENSITIVE
            );
    /**
     * Edité le : 16/06/26 à 15:01:11
     */
    private static final Pattern GENERATED_AT_PATTERN =
            Pattern.compile(
                    "Edit[ée]\\s*le\\s*:\\s*(\\d{2}/\\d{2}/\\d{2})\\s*[àa]\\s*(\\d{2}:\\d{2}:\\d{2})",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Volume :722 774,99
     */
    private static final Pattern VOLUME_PATTERN =
            Pattern.compile(
                    "Volume\\s*:?\\s*([\\d\\s.,]+)",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Nombre d'opérations:05
     */
    private static final Pattern COUNT_PATTERN =
            Pattern.compile(
                    "Nombre\\s+d['’]opérations\\s*:?\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Exemple ligne :
     *
     * 12542226975 Cameroun 16/06/2026 7:15AM CM50120 65000,15 XAF Ria Emission
     * 12542226975 France 16/06/2026 7:15AM MG1200 65000,15 EUR Money Gram Reception
     */
    private static final Pattern TRANSACTION_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s+" +                    // 1. Référence
                            "([A-Za-zÀ-ÿ\\s]+?)\\s+" +        // 2. Pays (plus flexible)
                            "(\\d{2}/\\d{2}/\\d{4})\\s+" +    // 3. Date
                            "(\\d{1,2}:\\d{2}[AP]M)\\s+" +    // 4. Heure
                            "([A-Z0-9]+)\\s+" +               // 5. Opérateur
                            "([\\d\\s.,]+)\\s+" +             // 6. Montant
                            "([A-Z]{3})\\s+" +                // 7. Devise
                            "(.+)$",                          // 8. Service
                    Pattern.MULTILINE
            );

    public ParsedReport parse(File pdfFile) throws IOException {
        String text;
        try (var document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
        }
        log.debug("Texte extrait :\n{}", text);

        String title = "Rapport sans titre";
        String agencyName = "Agence inconnue";
        LocalDate periodStart = LocalDate.now();
        LocalDate periodEnd = LocalDate.now();
        Matcher headerMatcher = HEADER_PATTERN.matcher(text);

        if (headerMatcher.find()) {

            title = headerMatcher.group(1).trim();

            agencyName = title
                    .replaceFirst("(?i)Rapport\\s+Agence\\s+", "")
                    .replaceFirst("(?i)\\s+pour\\s+le\\s+produit.*$", "")
                    .trim();

            periodStart = parseDate(headerMatcher.group(2));
            periodEnd = parseDate(headerMatcher.group(3));
        }

        String secretaryName =
                extract(text, SECRETARY_PATTERN, 1, "Inconnu");

        BigDecimal totalVolume =
                parseMontant(extract(text, VOLUME_PATTERN, 1, "0"));

        Integer transactionCount =
                parseInt(extract(text, COUNT_PATTERN, 1, "0"));

        LocalDateTime generatedAt = null;

        Matcher generatedMatcher = GENERATED_AT_PATTERN.matcher(text);

        if (generatedMatcher.find()) {

            String datePart = generatedMatcher.group(1);
            String timePart = generatedMatcher.group(2);

            generatedAt = LocalDateTime.parse(
                    datePart + " " + timePart,
                    DATETIME_FORMAT
            );
        }

        Report report = Report.builder()
                .title(title)
                .agencyName(agencyName)
                .receptionDate(
                        generatedAt != null
                                ? generatedAt.toLocalDate()
                                : LocalDate.now()
                )
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .secretaryName(secretaryName)
                .totalVolume(totalVolume)
                .transactionCount(transactionCount)
                .build();

        List<Transaction> transactions = new ArrayList<>();

        Matcher transactionMatcher =
                TRANSACTION_PATTERN.matcher(text);

        while (transactionMatcher.find()) {

            Transaction transaction = Transaction.builder()
                    .reference(transactionMatcher.group(1).trim())
                    .country(transactionMatcher.group(2).trim())
                    .date(parseDate(transactionMatcher.group(3).trim()))
                    .operator(transactionMatcher.group(5).trim())
                    .amount(parseMontant(transactionMatcher.group(6).trim()))
                    .currency(transactionMatcher.group(7).trim())
                    .service(transactionMatcher.group(8).trim())
                    .build();

            transactions.add(transaction);
        }

        log.info(
                "Rapport '{}' parsé avec {} transactions",
                title,
                transactions.size()
        );

        return new ParsedReport(report, transactions);
    }

    private String extract(
            String text,
            Pattern pattern,
            int group,
            String defaultValue
    ) {

        Matcher matcher = pattern.matcher(text);

        return matcher.find()
                ? matcher.group(group).trim()
                : defaultValue;
    }

    private LocalDate parseDate(String raw) {

        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (Exception ignored) {
            }
        }

        log.warn("Date non reconnue : {}", raw);

        return LocalDate.now();
    }

    private BigDecimal parseMontant(String raw) {

        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }

        String clean = raw
                .replaceAll("[\\s\\u00A0]", "")
                .replace(",", ".");

        int lastDot = clean.lastIndexOf('.');

        if (lastDot >= 0) {

            clean =
                    clean.substring(0, lastDot).replace(".", "")
                            + "."
                            + clean.substring(lastDot + 1);
        }

        try {
            return new BigDecimal(clean);
        } catch (Exception e) {

            log.warn("Montant non reconnu : {}", raw);

            return BigDecimal.ZERO;
        }
    }

    private Integer parseInt(String raw) {

        if (raw == null || raw.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    raw.replaceAll("\\D", "")
            );
        } catch (Exception e) {
            return 0;
        }
    }

    public record ParsedReport(
            Report report,
            List<Transaction> transactions
    ) { }
}