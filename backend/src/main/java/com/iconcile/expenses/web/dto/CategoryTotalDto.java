package com.iconcile.expenses.web.dto;

import java.math.BigDecimal;

public record CategoryTotalDto(Long categoryId, String category, String color, BigDecimal total, Long count) {
}
