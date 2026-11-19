package com.iconcile.expenses.service;

import com.iconcile.expenses.domain.CsvExpenseParser;
import com.iconcile.expenses.domain.VendorNormalizer;
import com.iconcile.expenses.entity.Category;
import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.entity.ExpenseSource;
import com.iconcile.expenses.repository.CategoryRepository;
import com.iconcile.expenses.repository.ExpenseRepository;
import com.iconcile.expenses.web.dto.CsvImportResultDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Imports a CSV in one transaction: valid rows are saved, invalid rows are reported with line numbers, and rows
 * that already exist in the database (same date + amount + vendor + description) are skipped so re-uploading the
 * same statement is harmless. Identical rows <em>within</em> one file are kept - two equal auto-rickshaw fares on
 * the same day are legitimate. Anomalies are recomputed once per affected
 * category at the end, not once per row.
 */
@Service
public class CsvImportService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;
    private final AnomalyService anomalyService;
    private final Clock clock;

    public CsvImportService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository,
                            CategorizationService categorizationService, AnomalyService anomalyService, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
        this.anomalyService = anomalyService;
        this.clock = clock;
    }

    @Transactional
    public CsvImportResultDto importCsv(String content) {
        CsvExpenseParser.Result parsed = new CsvExpenseParser(LocalDate.now(clock)).parse(content);
        List<CsvExpenseParser.ParsedRow> rows = parsed.rows();

        Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Function.identity()));
        Set<String> existing = existingKeys(rows);

        List<Expense> toSave = new ArrayList<>();
        int duplicates = 0;
        for (CsvExpenseParser.ParsedRow row : rows) {
            String key = key(row.date(), row.amount(), row.vendorName(), row.description());
            if (existing.contains(key)) {
                duplicates++;
                continue;
            }
            Expense e = new Expense(row.date(), row.amount(), row.vendorName(), row.description(), ExpenseSource.CSV);
            Category explicit = row.categoryName() == null ? null
                    : categoriesByName.get(row.categoryName().trim().toLowerCase(Locale.ROOT));
            if (explicit != null) {
                e.assignCategory(explicit, CategorySource.MANUAL);
            } else {
                // unknown or missing category column -> rule-based categorisation
                CategorizationService.Resolution r = categorizationService.resolve(row.vendorName());
                e.assignCategory(r.category(), r.source());
            }
            toSave.add(e);
        }

        expenseRepository.saveAll(toSave);
        expenseRepository.flush();
        anomalyService.recompute(toSave.stream().map(e -> e.getCategory().getId()).collect(Collectors.toSet()));

        int anomalies = (int) toSave.stream().filter(Expense::isAnomaly).count();
        return new CsvImportResultDto(parsed.totalDataRows(), toSave.size(), duplicates, parsed.errors().size(),
                anomalies, parsed.errors());
    }

    private Set<String> existingKeys(List<CsvExpenseParser.ParsedRow> rows) {
        Set<String> keys = new HashSet<>();
        if (rows.isEmpty()) {
            return keys;
        }
        LocalDate min = rows.stream().map(CsvExpenseParser.ParsedRow::date).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate max = rows.stream().map(CsvExpenseParser.ParsedRow::date).max(Comparator.naturalOrder()).orElseThrow();
        for (Expense e : expenseRepository.findByExpenseDateBetween(min, max)) {
            keys.add(key(e.getExpenseDate(), e.getAmount(), e.getVendorName(), e.getDescription()));
        }
        return keys;
    }

    private static String key(LocalDate date, BigDecimal amount, String vendor, String description) {
        return date + "|" + amount.stripTrailingZeros().toPlainString() + "|" + VendorNormalizer.normalize(vendor)
                + "|" + Objects.toString(description, "").trim().toLowerCase(Locale.ROOT);
    }
}
