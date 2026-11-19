package com.iconcile.expenses.entity;

import com.iconcile.expenses.domain.CategoryRule;
import com.iconcile.expenses.domain.MatchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vendor_rule")
public class VendorRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored normalised (see VendorNormalizer). */
    @Column(nullable = false, length = 255)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 16)
    private MatchType matchType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected VendorRule() {
    }

    public VendorRule(String pattern, MatchType matchType, Category category) {
        this.pattern = pattern;
        this.matchType = matchType;
        this.category = category;
    }

    public CategoryRule toDomain() {
        return new CategoryRule(id, pattern, matchType, category.getId());
    }

    public Long getId() {
        return id;
    }

    public String getPattern() {
        return pattern;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
