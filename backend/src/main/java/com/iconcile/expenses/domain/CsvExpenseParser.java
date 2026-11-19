package com.iconcile.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses an uploaded expense CSV into validated rows plus per-row errors.
 *
 * <ul>
 *   <li>RFC 4180 quoting (commas, escaped quotes and newlines inside quoted fields), UTF-8 BOM, CRLF/LF.</li>
 *   <li>Header names are case/space-insensitive with common aliases (e.g. "Vendor Name", "merchant").</li>
 *   <li>Required columns: date, amount, vendor. Optional: description, category.</li>
 *   <li>Dates: yyyy-MM-dd (ISO), dd/MM/yyyy, dd-MM-yyyy, d MMM yyyy. Slash dates are read day-first (Indian format).</li>
 *   <li>Amounts: may contain ₹ / Rs / INR prefixes and thousands separators; must be &gt; 0 with ≤ 2 decimals.</li>
 *   <li>Bad rows never abort the import: they are returned as {@link RowError}s with their line number.</li>
 * </ul>
 */
public final class CsvExpenseParser {

    public static final int MAX_ROWS = 5_000;
    static final int MAX_VENDOR_LENGTH = 255;
    static final int MAX_DESCRIPTION_LENGTH = 1_000;
    static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private static final Map<String, String> HEADER_ALIASES = new HashMap<>();

    static {
        alias("date", "date", "expense date", "transaction date", "txn date");
        alias("amount", "amount", "amount inr", "value", "debit");
        alias("vendor", "vendor", "vendor name", "merchant", "merchant name", "payee");
        alias("description", "description", "desc", "notes", "note", "narration", "remarks");
        alias("category", "category");
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu")
                    .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT));

    private static void alias(String canonical, String... names) {
        for (String n : names) {
            HEADER_ALIASES.put(n, canonical);
        }
    }

    public record ParsedRow(int lineNumber, LocalDate date, BigDecimal amount, String vendorName,
                            String description, String categoryName) {
    }

    public record RowError(int lineNumber, String message) {
    }

    public record Result(List<ParsedRow> rows, List<RowError> errors, int totalDataRows) {
    }

    /** Thrown when the file as a whole is unusable (empty, missing required columns, too many rows). */
    public static class CsvFormatException extends RuntimeException {
        public CsvFormatException(String message) {
            super(message);
        }
    }

    private final LocalDate today;

    /** @param today used to reject future-dated expenses; injected for testability. */
    public CsvExpenseParser(LocalDate today) {
        this.today = today;
    }

    public Result parse(String content) {
        if (content == null || content.isBlank()) {
            throw new CsvFormatException("The file is empty.");
        }
        if (content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        List<RawRecord> records = tokenize(content);
        if (records.isEmpty()) {
            throw new CsvFormatException("The file is empty.");
        }

        Map<String, Integer> columns = mapHeader(records.get(0).fields());
        int dataRows = records.size() - 1;
        if (dataRows == 0) {
            throw new CsvFormatException("The file has a header but no expense rows.");
        }
        if (dataRows > MAX_ROWS) {
            throw new CsvFormatException("Too many rows (" + dataRows + "). The limit is " + MAX_ROWS + " per upload.");
        }

        List<ParsedRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            RawRecord rec = records.get(i);
            try {
                rows.add(toRow(rec, columns));
            } catch (RowException e) {
                errors.add(new RowError(rec.lineNumber(), e.getMessage()));
            }
        }
        return new Result(rows, errors, dataRows);
    }

    private Map<String, Integer> mapHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = header.get(i).trim().toLowerCase(Locale.ROOT).replaceAll("[_\\s]+", " ");
            String canonical = HEADER_ALIASES.get(key);
            if (canonical != null) {
                columns.putIfAbsent(canonical, i);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String required : List.of("date", "amount", "vendor")) {
            if (!columns.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new CsvFormatException("Missing required column(s): " + String.join(", ", missing)
                    + ". Expected header like: date,amount,vendor,description");
        }
        return columns;
    }

    private ParsedRow toRow(RawRecord rec, Map<String, Integer> columns) {
        String dateRaw = field(rec, columns, "date");
        String amountRaw = field(rec, columns, "amount");
        String vendor = field(rec, columns, "vendor");
        String description = field(rec, columns, "description");
        String category = field(rec, columns, "category");

        List<String> problems = new ArrayList<>();
        LocalDate date = null;
        BigDecimal amount = null;

        if (dateRaw.isEmpty()) {
            problems.add("date is required");
        } else {
            date = parseDate(dateRaw);
            if (date == null) {
                problems.add("invalid date '" + dateRaw + "' (use YYYY-MM-DD or DD/MM/YYYY)");
            } else if (date.isAfter(today)) {
                problems.add("date " + date + " is in the future");
            }
        }

        if (amountRaw.isEmpty()) {
            problems.add("amount is required");
        } else {
            try {
                amount = parseAmount(amountRaw);
            } catch (IllegalArgumentException e) {
                problems.add(e.getMessage());
            }
        }

        if (vendor.isEmpty()) {
            problems.add("vendor is required");
        } else if (vendor.length() > MAX_VENDOR_LENGTH) {
            problems.add("vendor is longer than " + MAX_VENDOR_LENGTH + " characters");
        } else if (VendorNormalizer.normalize(vendor).isEmpty()) {
            problems.add("vendor must contain letters or digits");
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            problems.add("description is longer than " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        if (!problems.isEmpty()) {
            throw new RowException(String.join("; ", problems));
        }
        return new ParsedRow(rec.lineNumber(), date, amount, vendor, description.isEmpty() ? null : description,
                category.isEmpty() ? null : category);
    }

    private static String field(RawRecord rec, Map<String, Integer> columns, String name) {
        Integer idx = columns.get(name);
        if (idx == null || idx >= rec.fields().size()) {
            return "";
        }
        return rec.fields().get(idx).trim();
    }

    static LocalDate parseDate(String raw) {
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, f);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    static BigDecimal parseAmount(String raw) {
        String cleaned = raw.replaceAll("(?i)^(₹|rs\\.?|inr)\\s*", "").replace(",", "").trim();
        BigDecimal value;
        try {
            value = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid amount '" + raw + "'");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0 (got " + raw + ")");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("amount '" + raw + "' has more than 2 decimal places");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("amount '" + raw + "' is too large");
        }
        return value.setScale(2);
    }

    // ---------------------------------------------------------------- RFC 4180 tokenizer

    record RawRecord(int lineNumber, List<String> fields) {
    }

    /** Splits CSV text into records, honouring quotes. Blank lines are skipped. Line numbers are 1-based. */
    static List<RawRecord> tokenize(String content) {
        List<RawRecord> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int line = 1;
        int recordStartLine = 1;
        int len = content.length();

        for (int i = 0; i < len; i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < len && content.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(current.toString());
                current.setLength(0);
                addIfNotBlank(records, recordStartLine, fields);
                fields = new ArrayList<>();
                line++;
                recordStartLine = line;
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new CsvFormatException("Unterminated quoted field starting on line " + recordStartLine + ".");
        }
        fields.add(current.toString());
        addIfNotBlank(records, recordStartLine, fields);
        return records;
    }

    private static void addIfNotBlank(List<RawRecord> records, int line, List<String> fields) {
        boolean blank = fields.stream().allMatch(f -> f.isBlank());
        if (!blank) {
            records.add(new RawRecord(line, List.copyOf(fields)));
        }
    }

    private static class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }
}
