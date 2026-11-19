package com.iconcile.expenses.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, thread-safe rule engine that maps a vendor name to a category.
 *
 * <p>Resolution order (most specific wins):
 * <ol>
 *   <li>EXACT rule whose pattern equals the normalised vendor name.</li>
 *   <li>CONTAINS rule whose tokens occur as a contiguous word sequence in the vendor name.
 *       If several match, the one with the most tokens wins ("uber eats" → Food beats "uber" → Transport),
 *       then the longest pattern, then the oldest rule (lowest id) for determinism.</li>
 *   <li>No match → {@link Optional#empty()}; the caller falls back to the default category.</li>
 * </ol>
 * Build once from the rule table and reuse; rebuild when rules change.
 */
public final class VendorCategorizer {

    private static final Comparator<CompiledRule> SPECIFICITY = Comparator
            .comparingInt((CompiledRule r) -> r.tokens.size())
            .thenComparingInt(r -> r.rule.pattern().length())
            .thenComparing(r -> -r.rule.ruleId());

    private final Map<String, CategoryRule> exactRules = new HashMap<>();
    private final List<CompiledRule> containsRules = new ArrayList<>();

    public VendorCategorizer(List<CategoryRule> rules) {
        for (CategoryRule rule : rules) {
            String normalized = VendorNormalizer.normalize(rule.pattern());
            if (normalized.isEmpty()) {
                continue;
            }
            if (rule.matchType() == MatchType.EXACT) {
                exactRules.merge(normalized, rule, (a, b) -> a.ruleId() <= b.ruleId() ? a : b);
            } else {
                containsRules.add(new CompiledRule(rule, VendorNormalizer.tokens(normalized)));
            }
        }
    }

    public Optional<CategoryRule> match(String vendorName) {
        String normalized = VendorNormalizer.normalize(vendorName);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        CategoryRule exact = exactRules.get(normalized);
        if (exact != null) {
            return Optional.of(exact);
        }
        List<String> vendorTokens = VendorNormalizer.tokens(normalized);
        return containsRules.stream()
                .filter(r -> containsSequence(vendorTokens, r.tokens))
                .max(SPECIFICITY)
                .map(r -> r.rule);
    }

    static boolean containsSequence(List<String> haystack, List<String> needle) {
        if (needle.isEmpty() || needle.size() > haystack.size()) {
            return false;
        }
        outer:
        for (int i = 0; i <= haystack.size() - needle.size(); i++) {
            for (int j = 0; j < needle.size(); j++) {
                if (!haystack.get(i + j).equals(needle.get(j))) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private record CompiledRule(CategoryRule rule, List<String> tokens) {
    }
}
