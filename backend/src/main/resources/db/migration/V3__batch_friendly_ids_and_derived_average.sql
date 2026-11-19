-- Expense ids now come from a pooled sequence (Hibernate allocationSize = 50) instead of IDENTITY, because
-- IDENTITY forces one INSERT round trip per row and silently disables JDBC batching for CSV imports.
-- The column's DEFAULT nextval(...) stays, so hand-written INSERTs still work.
ALTER SEQUENCE expense_id_seq INCREMENT BY 50;

-- The peer average shown next to a flagged expense is now derived at read time from per-category count/sum.
-- Storing it meant every new expense rewrote every row in its category (the average moves on each write).
ALTER TABLE expense DROP COLUMN category_average;
