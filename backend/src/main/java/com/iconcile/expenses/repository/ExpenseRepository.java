package com.iconcile.expenses.repository;

import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.web.dto.CategoryTotalDto;
import com.iconcile.expenses.web.dto.VendorTotalDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /** One query for every category an operation touched (instead of one query per category). */
    List<Expense> findByCategoryIdIn(Collection<Long> categoryIds);

    /** One row per category; the number of rows is bounded by the number of categories, not of expenses. */
    @Query("""
            select new com.iconcile.expenses.repository.CategoryStats(e.category.id, count(e), sum(e.amount))
            from Expense e
            group by e.category.id
            """)
    List<CategoryStats> categoryStats();

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

    List<Expense> findByCategorySourceNot(CategorySource source);

    List<Expense> findByExpenseDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "category")
    List<Expense> findByAnomalyTrueAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(LocalDate from, LocalDate to);

    @Query("""
            select new com.iconcile.expenses.web.dto.CategoryTotalDto(c.id, c.name, c.color, sum(e.amount), count(e))
            from Expense e join e.category c
            where e.expenseDate between :from and :to
            group by c.id, c.name, c.color
            order by sum(e.amount) desc
            """)
    List<CategoryTotalDto> categoryTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new com.iconcile.expenses.web.dto.VendorTotalDto(min(e.vendorName), sum(e.amount), count(e))
            from Expense e
            where e.expenseDate between :from and :to
            group by e.vendorNormalized
            order by sum(e.amount) desc, count(e) desc
            """)
    List<VendorTotalDto> topVendors(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    interface MonthlyCategoryTotalView {
        String getMonth();

        String getCategory();

        String getColor();

        BigDecimal getTotal();
    }

    @Query(value = """
            select to_char(e.expense_date, 'YYYY-MM') as month, c.name as category, c.color as color,
                   sum(e.amount) as total
            from expense e join category c on c.id = e.category_id
            where e.expense_date between :from and :to
            group by 1, 2, 3
            order by 1, 4 desc
            """, nativeQuery = true)
    List<MonthlyCategoryTotalView> monthlyCategoryTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "select distinct to_char(expense_date, 'YYYY-MM') as m from expense order by m desc",
            nativeQuery = true)
    List<String> distinctMonths();
}
