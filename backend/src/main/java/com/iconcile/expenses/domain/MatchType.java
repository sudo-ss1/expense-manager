package com.iconcile.expenses.domain;

/**
 * EXACT    – the whole normalised vendor name must equal the pattern ("amazon" matches "Amazon" but not "Amazon Pay").
 * CONTAINS – the pattern's tokens must appear as a contiguous word sequence inside the vendor name
 *            ("uber eats" matches "Uber Eats Order #12"; "uber" does NOT match "Tuber Farms").
 */
public enum MatchType {
    EXACT,
    CONTAINS
}
