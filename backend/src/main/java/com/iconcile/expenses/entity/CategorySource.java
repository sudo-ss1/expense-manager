package com.iconcile.expenses.entity;

/** How an expense got its category - lets us re-apply rules without overwriting user choices. */
public enum CategorySource {
    /** Matched a vendor rule. */
    RULE,
    /** Chosen explicitly by the user (form or CSV "category" column). Never overwritten by rules. */
    MANUAL,
    /** No rule matched; fell back to the default category. */
    DEFAULT
}
