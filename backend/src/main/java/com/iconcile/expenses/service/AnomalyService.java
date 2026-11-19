package com.iconcile.expenses.service;

import com.iconcile.expenses.domain.AnomalyDetector;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.repository.CategoryStats;
import com.iconcile.expenses.repository.ExpenseRepository;
import com.iconcile.expenses.web.dto.ExpenseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps the persisted {@code is_anomaly} flag consistent. Any write that changes a category's set of amounts
 * (add, edit, delete, CSV import, re-categorisation) must call {@link #recompute(Collection)} for the affected
 * categories inside the same transaction - because adding one expense shifts the average for every other
 * expense in that category, flags of older expenses can change too.
 *
 * <p>Query budget: a recompute is one SELECT for all affected categories plus UPDATEs only for expenses whose flag
 * flipped. Building API responses costs one extra aggregate query ({@link #toDto()}), whatever the page size.
 */
@Service
public class AnomalyService {

    private final ExpenseRepository expenseRepository;
    private final AnomalyDetector detector;

    public AnomalyService(ExpenseRepository expenseRepository, AnomalyDetector detector) {
        this.expenseRepository = expenseRepository;
        this.detector = detector;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recompute(Collection<Long> categoryIds) {
        List<Long> ids = categoryIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        // getCategory().getId() reads the id from the lazy proxy - no extra query per expense
        Map<Long, List<Expense>> byCategory = expenseRepository.findByCategoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().getId()));
        for (List<Expense> expenses : byCategory.values()) {
            Map<Long, Expense> byId = expenses.stream().collect(Collectors.toMap(Expense::getId, Function.identity()));
            List<AnomalyDetector.Observation<Long>> observations = expenses.stream()
                    .map(e -> new AnomalyDetector.Observation<>(e.getId(), e.getAmount()))
                    .toList();
            for (AnomalyDetector.Verdict<Long> v : detector.evaluate(observations)) {
                byId.get(v.id()).setAnomaly(v.anomaly());
            }
        }
    }

    /**
     * Returns a mapper that turns expenses into API DTOs, including the "why flagged" baseline. Category sizes and
     * totals are loaded once, so mapping a page of N expenses costs one query, not N. Call inside a transaction,
     * after the write (the query auto-flushes pending changes, so the baseline reflects them). Not annotated: the
     * returned function touches lazy associations, so it must run in the caller's transaction anyway.
     */
    public Function<Expense, ExpenseDto> toDto() {
        Map<Long, CategoryStats> stats = expenseRepository.categoryStats().stream()
                .collect(Collectors.toMap(CategoryStats::categoryId, Function.identity()));
        return e -> {
            CategoryStats s = stats.get(e.getCategory().getId());
            BigDecimal baseline = s == null ? null : detector.baseline(e.getAmount(), s.count(), s.sum());
            return ExpenseDto.from(e, baseline);
        };
    }
}
