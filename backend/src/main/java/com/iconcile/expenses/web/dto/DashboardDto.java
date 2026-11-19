package com.iconcile.expenses.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param month         selected month (YYYY-MM) or null for all time
 * @param monthlyTrend  per-month, per-category totals for the 6 months ending at the selected / latest month
 */
public record DashboardDto(String month, List<String> availableMonths, BigDecimal totalSpend, long expenseCount,
                           List<CategoryTotalDto> categoryTotals, List<VendorTotalDto> topVendors,
                           long anomalyCount, List<ExpenseDto> anomalies,
                           List<MonthlyCategoryTotalDto> monthlyTrend) {
}
