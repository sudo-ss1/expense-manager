package com.iconcile.expenses.service;

import com.iconcile.expenses.entity.Category;
import com.iconcile.expenses.repository.CategoryRepository;
import com.iconcile.expenses.web.dto.CategoryDto;
import com.iconcile.expenses.web.dto.CategoryRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Category '" + name + "' already exists");
        }
        String color = req.color() == null ? "#64748b" : req.color();
        return CategoryDto.from(categoryRepository.save(new Category(name, color)));
    }
}
