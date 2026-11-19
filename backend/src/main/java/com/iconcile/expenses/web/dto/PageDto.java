package com.iconcile.expenses.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON contract for paged results (serialising Spring's PageImpl directly is discouraged). */
public record PageDto<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber(),
                page.getSize());
    }
}
