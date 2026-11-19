package com.iconcile.expenses.repository;

import com.iconcile.expenses.domain.MatchType;
import com.iconcile.expenses.entity.VendorRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VendorRuleRepository extends JpaRepository<VendorRule, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("select r from VendorRule r order by r.category.name, r.pattern")
    List<VendorRule> findAllWithCategory();

    Optional<VendorRule> findByPatternAndMatchType(String pattern, MatchType matchType);
}
