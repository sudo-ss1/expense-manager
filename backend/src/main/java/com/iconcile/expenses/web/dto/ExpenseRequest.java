package com.iconcile.expenses.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param categoryId     optional explicit category; when null the vendor rules decide
 * @param rememberVendor when true together with categoryId, an EXACT vendor rule is saved so future
 *                       expenses from this vendor are categorised the same way automatically
 */
public record ExpenseRequest(
        @NotNull(message = "date is required") @PastOrPresent(message = "date cannot be in the future") LocalDate date,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "amount must be at most 9,999,999,999.99 with at most 2 decimal places") BigDecimal amount,
        @NotBlank(message = "vendor name is required") @Size(max = 255)
        @Pattern(regexp = ".*[\\p{L}\\p{N}].*", message = "vendor name must contain letters or digits") String vendorName,
        @Size(max = 1000) String description,
        Long categoryId,
        boolean rememberVendor) {
}
