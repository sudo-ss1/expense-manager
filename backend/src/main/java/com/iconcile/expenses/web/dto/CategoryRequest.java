package com.iconcile.expenses.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 64) String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex value like #3b82f6") String color) {
}
