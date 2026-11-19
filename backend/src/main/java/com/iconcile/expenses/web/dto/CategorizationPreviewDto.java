package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.entity.CategorySource;

/** What the rule engine would pick for a vendor - shown live while the user types. */
public record CategorizationPreviewDto(Long categoryId, String categoryName, String categoryColor,
                                       CategorySource source, String matchedPattern) {
}
