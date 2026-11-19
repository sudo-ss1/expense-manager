package com.iconcile.expenses.service;

import com.iconcile.expenses.entity.Category;
import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.entity.ExpenseSource;
import com.iconcile.expenses.repository.CategoryRepository;
import com.iconcile.expenses.repository.ExpenseRepository;
import com.iconcile.expenses.web.dto.CategorizationPreviewDto;
import com.iconcile.expenses.web.dto.ExpenseDto;
import com.iconcile.expenses.web.dto.ExpenseRequest;
import com.iconcile.expenses.web.dto.PageDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;
    private final AnomalyService anomalyService;
    private final RuleService ruleService;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository,
                          CategorizationService categorizationService, AnomalyService anomalyService,
                          RuleService ruleService) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
        this.anomalyService = anomalyService;
        this.ruleService = ruleService;
    }

    @Transactional
    public ExpenseDto create(ExpenseRequest req) {
        Expense expense = new Expense(req.date(), req.amount(), req.vendorName(), blankToNull(req.description()),
                ExpenseSource.MANUAL);
        applyCategory(expense, req);
        expenseRepository.save(expense);
        anomalyService.recompute(List.of(expense.getCategory().getId()));
        return anomalyService.toDto().apply(expense);
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseRequest req) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
        Long oldCategoryId = expense.getCategory().getId();
        expense.setDate(req.date());
        expense.setAmount(req.amount());
        expense.setVendorName(req.vendorName());
        expense.setDescription(blankToNull(req.description()));
        // An edit that doesn't name a category keeps a manually chosen one; rules never overwrite user intent.
        if (req.categoryId() != null || expense.getCategorySource() != CategorySource.MANUAL) {
            applyCategory(expense, req);
        }
        anomalyService.recompute(List.of(oldCategoryId, expense.getCategory().getId()));
        return anomalyService.toDto().apply(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
        Long categoryId = expense.getCategory().getId();
        expenseRepository.delete(expense);
        expenseRepository.flush();
        anomalyService.recompute(List.of(categoryId));
    }

    @Transactional(readOnly = true)
    public PageDto<ExpenseDto> list(YearMonth month, LocalDate from, LocalDate to, Long categoryId,
                                 boolean anomaliesOnly, String search, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' (" + from + ") must be on or before 'to' (" + to + ")");
        }
        List<Specification<Expense>> specs = new ArrayList<>();
        if (month != null) {
            specs.add((root, q, cb) -> cb.between(root.get("expenseDate"), month.atDay(1), month.atEndOfMonth()));
        }
        if (from != null) {
            specs.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            specs.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        if (categoryId != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (anomaliesOnly) {
            specs.add((root, q, cb) -> cb.isTrue(root.get("anomaly")));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            specs.add((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("vendorName")), like, '\\'),
                    cb.like(cb.lower(root.get("description")), like, '\\')));
        }
        Specification<Expense> spec = Specification.allOf(specs);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
        return PageDto.from(expenseRepository.findAll(spec, pageRequest).map(anomalyService.toDto()));
    }

    @Transactional(readOnly = true)
    public CategorizationPreviewDto preview(String vendorName) {
        CategorizationService.Resolution r = categorizationService.resolve(vendorName);
        Category c = r.category();
        return new CategorizationPreviewDto(c.getId(), c.getName(), c.getColor(), r.source(), r.matchedPattern());
    }

    /** Explicit category from the user wins; otherwise the vendor rules decide. */
    private void applyCategory(Expense expense, ExpenseRequest req) {
        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category " + req.categoryId() + " not found"));
            expense.assignCategory(category, CategorySource.MANUAL);
            if (req.rememberVendor()) {
                ruleService.upsertExactRule(req.vendorName(), category);
            }
        } else {
            CategorizationService.Resolution r = categorizationService.resolve(req.vendorName());
            expense.assignCategory(r.category(), r.source());
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
