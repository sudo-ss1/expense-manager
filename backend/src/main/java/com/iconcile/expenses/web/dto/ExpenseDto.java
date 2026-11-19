package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.entity.ExpenseSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * @param categoryAverage current average of the other expenses in this category (null if too few to judge)
 * @param anomalyRatio    amount / categoryAverage, e.g. 4.2 means "4.2x the usual" (null if not evaluated)
 */
public record ExpenseDto(Long id, LocalDate date, BigDecimal amount, String vendorName, String description,
                         Long categoryId, String categoryName, String categoryColor, CategorySource categorySource,
                         ExpenseSource source, boolean anomaly, BigDecimal categoryAverage, BigDecimal anomalyRatio) {

    /** @param avg leave-one-out category average, derived by {@code AnomalyService#toDto()} */
    public static ExpenseDto from(Expense e, BigDecimal avg) {
        BigDecimal ratio = (avg == null || avg.signum() == 0) ? null
                : e.getAmount().divide(avg, 1, RoundingMode.HALF_UP);
        return new ExpenseDto(e.getId(), e.getExpenseDate(), e.getAmount(), e.getVendorName(), e.getDescription(),
                e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getColor(),
                e.getCategorySource(), e.getSource(), e.isAnomaly(), avg, ratio);
    }
}
