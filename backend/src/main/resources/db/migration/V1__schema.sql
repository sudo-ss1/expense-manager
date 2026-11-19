-- Categories: small lookup table. Exactly one row is the fallback for vendors that match no rule.
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    color       VARCHAR(16)  NOT NULL DEFAULT '#64748b',
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_category_name ON category (lower(name));
CREATE UNIQUE INDEX uq_category_single_default ON category (is_default) WHERE is_default;

-- Vendor -> category mapping rules. Patterns are stored already normalised (lower-case, punctuation stripped).
CREATE TABLE vendor_rule (
    id           BIGSERIAL PRIMARY KEY,
    pattern      VARCHAR(255) NOT NULL,
    match_type   VARCHAR(16)  NOT NULL CHECK (match_type IN ('EXACT', 'CONTAINS')),
    category_id  BIGINT       NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vendor_rule UNIQUE (pattern, match_type)
);

CREATE TABLE expense (
    id                BIGSERIAL PRIMARY KEY,
    expense_date      DATE           NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    vendor_name       VARCHAR(255)   NOT NULL,
    vendor_normalized VARCHAR(255)   NOT NULL,   -- grouping key for "top vendors"
    description       VARCHAR(1000),
    category_id       BIGINT         NOT NULL REFERENCES category (id),
    category_source   VARCHAR(16)    NOT NULL CHECK (category_source IN ('RULE', 'MANUAL', 'DEFAULT')),
    source            VARCHAR(16)    NOT NULL CHECK (source IN ('MANUAL', 'CSV')),
    is_anomaly        BOOLEAN        NOT NULL DEFAULT FALSE,
    category_average  NUMERIC(12, 2),            -- peer average used for the last anomaly check (explains the flag)
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_expense_date ON expense (expense_date);
CREATE INDEX idx_expense_category ON expense (category_id);
CREATE INDEX idx_expense_vendor ON expense (vendor_normalized);
CREATE INDEX idx_expense_anomaly ON expense (expense_date) WHERE is_anomaly;
