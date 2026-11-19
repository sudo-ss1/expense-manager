package com.iconcile.expenses.web.dto;

import java.math.BigDecimal;

public record VendorTotalDto(String vendor, BigDecimal total, Long count) {
}
