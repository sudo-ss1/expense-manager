package com.iconcile.expenses.entity;

import com.iconcile.expenses.domain.VendorNormalizer;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
public class Expense {

    /**
     * Pooled sequence (not IDENTITY): Hibernate reserves 50 ids per round trip, which lets CSV imports be sent as
     * JDBC batches. IDENTITY would force one INSERT per row. allocationSize must equal the INCREMENT BY in V3.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "expense_id")
    @SequenceGenerator(name = "expense_id", sequenceName = "expense_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Column(name = "vendor_normalized", nullable = false, length = 255)
    private String vendorNormalized;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 16)
    private CategorySource categorySource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExpenseSource source;

    @Column(name = "is_anomaly", nullable = false)
    private boolean anomaly;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Expense() {
    }

    public Expense(LocalDate date, BigDecimal amount, String vendorName, String description, ExpenseSource source) {
        this.expenseDate = date;
        this.amount = amount;
        setVendorName(vendorName);
        this.description = description;
        this.source = source;
    }

    public void assignCategory(Category category, CategorySource categorySource) {
        this.category = category;
        this.categorySource = categorySource;
    }

    /** Only a flip makes the row dirty, so a recompute UPDATEs just the expenses whose flag actually changed. */
    public void setAnomaly(boolean anomaly) {
        this.anomaly = anomaly;
    }

    public final void setVendorName(String vendorName) {
        this.vendorName = vendorName.trim();
        this.vendorNormalized = VendorNormalizer.normalize(vendorName);
    }

    public void setDate(LocalDate date) {
        this.expenseDate = date;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorNormalized() {
        return vendorNormalized;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public CategorySource getCategorySource() {
        return categorySource;
    }

    public ExpenseSource getSource() {
        return source;
    }

    public boolean isAnomaly() {
        return anomaly;
    }
}
