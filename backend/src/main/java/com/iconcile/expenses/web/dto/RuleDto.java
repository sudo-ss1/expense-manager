package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.domain.MatchType;
import com.iconcile.expenses.entity.VendorRule;

public record RuleDto(Long id, String pattern, MatchType matchType, Long categoryId, String categoryName,
                      String categoryColor) {
    public static RuleDto from(VendorRule r) {
        return new RuleDto(r.getId(), r.getPattern(), r.getMatchType(), r.getCategory().getId(),
                r.getCategory().getName(), r.getCategory().getColor());
    }
}
