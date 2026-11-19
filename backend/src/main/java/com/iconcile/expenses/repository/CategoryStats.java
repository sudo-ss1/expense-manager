package com.iconcile.expenses.repository;

import java.math.BigDecimal;

/** All-time size and total of one category - enough to derive any expense's leave-one-out baseline. */
public record CategoryStats(Long categoryId, Long count, BigDecimal sum) {
}
