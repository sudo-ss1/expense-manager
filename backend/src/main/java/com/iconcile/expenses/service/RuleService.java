package com.iconcile.expenses.service;

import com.iconcile.expenses.domain.MatchType;
import com.iconcile.expenses.domain.VendorNormalizer;
import com.iconcile.expenses.entity.Category;
import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.Expense;
import com.iconcile.expenses.entity.VendorRule;
import com.iconcile.expenses.repository.CategoryRepository;
import com.iconcile.expenses.repository.ExpenseRepository;
import com.iconcile.expenses.repository.VendorRuleRepository;
import com.iconcile.expenses.web.dto.ReapplyResultDto;
import com.iconcile.expenses.web.dto.RuleDto;
import com.iconcile.expenses.web.dto.RuleRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RuleService {

    private final VendorRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CategorizationService categorizationService;
    private final AnomalyService anomalyService;

    public RuleService(VendorRuleRepository ruleRepository, CategoryRepository categoryRepository,
                       ExpenseRepository expenseRepository, CategorizationService categorizationService,
                       AnomalyService anomalyService) {
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.categorizationService = categorizationService;
        this.anomalyService = anomalyService;
    }

    @Transactional(readOnly = true)
    public List<RuleDto> list() {
        return ruleRepository.findAllWithCategory().stream().map(RuleDto::from).toList();
    }

    /** Creates a rule, or re-points an existing rule with the same pattern + match type to the new category. */
    @Transactional
    public RuleDto upsert(RuleRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category " + req.categoryId() + " not found"));
        return RuleDto.from(save(req.pattern(), req.matchType(), category));
    }

    @Transactional
    public void upsertExactRule(String vendorName, Category category) {
        save(vendorName, MatchType.EXACT, category);
    }

    @Transactional
    public void delete(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new NotFoundException("Rule " + id + " not found");
        }
        ruleRepository.deleteById(id);
        categorizationService.invalidate();
    }

    /**
     * Re-runs the rules over every expense whose category was NOT chosen manually (manual choices are respected),
     * then recomputes anomalies for every category whose membership changed.
     */
    @Transactional
    public ReapplyResultDto reapplyToExisting() {
        List<Expense> candidates = expenseRepository.findByCategorySourceNot(CategorySource.MANUAL);
        Set<Long> touched = new HashSet<>();
        int changed = 0;
        for (Expense e : candidates) {
            CategorizationService.Resolution r = categorizationService.resolve(e.getVendorName());
            Long before = e.getCategory().getId();
            if (!Objects.equals(before, r.category().getId()) || e.getCategorySource() != r.source()) {
                e.assignCategory(r.category(), r.source());
                touched.add(before);
                touched.add(r.category().getId());
                changed++;
            }
        }
        expenseRepository.flush();
        anomalyService.recompute(touched);
        return new ReapplyResultDto(candidates.size(), changed);
    }

    private VendorRule save(String rawPattern, MatchType matchType, Category category) {
        String pattern = VendorNormalizer.normalize(rawPattern);
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Rule pattern must contain letters or digits");
        }
        VendorRule rule = ruleRepository.findByPatternAndMatchType(pattern, matchType)
                .map(existing -> {
                    existing.setCategory(category);
                    return existing;
                })
                .orElseGet(() -> ruleRepository.save(new VendorRule(pattern, matchType, category)));
        categorizationService.invalidate();
        return rule;
    }
}
