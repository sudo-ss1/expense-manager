package com.iconcile.expenses.web.dto;

import java.math.BigDecimal;

public record MonthlyCategoryTotalDto(String month, String category, String color, BigDecimal total) {
}
