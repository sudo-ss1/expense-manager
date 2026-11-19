package com.iconcile.expenses.domain;

/** Framework-free view of a vendor → category mapping rule. */
public record CategoryRule(long ruleId, String pattern, MatchType matchType, long categoryId) {
}
