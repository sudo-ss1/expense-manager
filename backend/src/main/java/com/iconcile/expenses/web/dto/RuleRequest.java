package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.domain.MatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RuleRequest(@NotBlank @Size(max = 255) String pattern,
                          @NotNull MatchType matchType,
                          @NotNull Long categoryId) {
}
