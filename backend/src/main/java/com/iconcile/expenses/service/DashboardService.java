package com.iconcile.expenses.service;

import com.iconcile.expenses.repository.ExpenseRepository;
import com.iconcile.expenses.web.dto.CategoryTotalDto;
import com.iconcile.expenses.web.dto.DashboardDto;
import com.iconcile.expenses.web.dto.ExpenseDto;
import com.iconcile.expenses.web.dto.MonthlyCategoryTotalDto;
import com.iconcile.expenses.web.dto.VendorTotalDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class DashboardService {

    private static final LocalDate ALL_TIME_FROM = LocalDate.of(1900, 1, 1);
    private static final LocalDate ALL_TIME_TO = LocalDate.of(9999, 12, 31);
    private static final int TOP_VENDORS = 5;
    private static final int TREND_MONTHS = 6;

    private final ExpenseRepository expenseRepository;
    private final AnomalyService anomalyService;
    private final Clock clock;

    public DashboardService(ExpenseRepository expenseRepository, AnomalyService anomalyService, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.anomalyService = anomalyService;
        this.clock = clock;
    }

    /** @param month month to summarise, or null for all time */
    @Transactional(readOnly = true)
    public DashboardDto summary(YearMonth month) {
        LocalDate from = month == null ? ALL_TIME_FROM : month.atDay(1);
        LocalDate to = month == null ? ALL_TIME_TO : month.atEndOfMonth();

        List<CategoryTotalDto> categoryTotals = expenseRepository.categoryTotals(from, to);
        BigDecimal total = categoryTotals.stream().map(CategoryTotalDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = categoryTotals.stream().mapToLong(CategoryTotalDto::count).sum();
        List<VendorTotalDto> topVendors = expenseRepository.topVendors(from, to, PageRequest.of(0, TOP_VENDORS));
        List<ExpenseDto> anomalies = expenseRepository
                .findByAnomalyTrueAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(from, to).stream()
                .map(anomalyService.toDto())
                .toList();

        List<String> months = expenseRepository.distinctMonths();
        YearMonth trendEnd = month != null ? month
                : months.isEmpty() ? YearMonth.now(clock) : YearMonth.parse(months.get(0));
        List<MonthlyCategoryTotalDto> trend = expenseRepository
                .monthlyCategoryTotals(trendEnd.minusMonths(TREND_MONTHS - 1L).atDay(1), trendEnd.atEndOfMonth())
                .stream()
                .map(v -> new MonthlyCategoryTotalDto(v.getMonth(), v.getCategory(), v.getColor(), v.getTotal()))
                .toList();

        return new DashboardDto(month == null ? null : month.toString(), months, total, count, categoryTotals,
                topVendors, anomalies.size(), anomalies, trend);
    }
}
