package com.iconcile.expenses.repository;

import com.iconcile.expenses.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByDefaultCategoryTrue();

    Optional<Category> findByNameIgnoreCase(String name);
}
