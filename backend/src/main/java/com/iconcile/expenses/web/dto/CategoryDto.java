package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.entity.Category;

public record CategoryDto(Long id, String name, String color, boolean isDefault) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getColor(), c.isDefaultCategory());
    }
}
