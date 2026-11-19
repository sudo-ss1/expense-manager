package com.iconcile.expenses.service;

import com.iconcile.expenses.domain.CategoryRule;
import com.iconcile.expenses.domain.VendorCategorizer;
import com.iconcile.expenses.entity.Category;
import com.iconcile.expenses.entity.CategorySource;
import com.iconcile.expenses.entity.VendorRule;
import com.iconcile.expenses.repository.CategoryRepository;
import com.iconcile.expenses.repository.VendorRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

/**
 * Applies vendor rules. The compiled {@link VendorCategorizer} is cached in memory and rebuilt lazily after any
 * rule change, so categorising a 5,000-row CSV costs one rules query, not one per row.
 */
@Service
public class CategorizationService {

    public record Resolution(Category category, CategorySource source, String matchedPattern) {
    }

    private final VendorRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private volatile VendorCategorizer cached;
    private volatile Long defaultCategoryId;

    public CategorizationService(VendorRuleRepository ruleRepository, CategoryRepository categoryRepository) {
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Called from within the caller's transaction (expense create / CSV import / re-apply / preview). */
    public Resolution resolve(String vendorName) {
        Optional<CategoryRule> match = categorizer().match(vendorName);
        if (match.isPresent()) {
            // getReferenceById: no extra query inside a transaction that already loaded categories
            Category category = categoryRepository.getReferenceById(match.get().categoryId());
            return new Resolution(category, CategorySource.RULE, match.get().pattern());
        }
        return new Resolution(defaultCategory(), CategorySource.DEFAULT, null);
    }

    /**
     * The fallback category's id is looked up once and cached (no API can change which category is the default),
     * then returned as a reference - so categorising N unmatched vendors costs no queries, not N.
     */
    public Category defaultCategory() {
        Long id = defaultCategoryId;
        if (id == null) {
            id = categoryRepository.findByDefaultCategoryTrue()
                    .orElseThrow(() -> new IllegalStateException("No default category configured"))
                    .getId();
            defaultCategoryId = id;
        }
        return categoryRepository.getReferenceById(id);
    }

    /**
     * Drops the compiled rules now (so later lookups in the same transaction see the change) and again after
     * commit (so a concurrent request that rebuilt the cache from pre-commit data doesn't keep it).
     */
    public void invalidate() {
        cached = null;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    cached = null;
                }
            });
        }
    }

    private VendorCategorizer categorizer() {
        VendorCategorizer local = cached;
        if (local == null) {
            List<CategoryRule> rules = ruleRepository.findAllWithCategory().stream().map(VendorRule::toDomain).toList();
            local = new VendorCategorizer(rules);
            cached = local;
        }
        return local;
    }
}
