package com.iconcile.expenses.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorCategorizerTest {

    private static final long FOOD = 1, TRANSPORT = 2, GROCERIES = 3, SHOPPING = 4;

    private final VendorCategorizer categorizer = new VendorCategorizer(List.of(
            new CategoryRule(1, "swiggy", MatchType.CONTAINS, FOOD),
            new CategoryRule(2, "uber", MatchType.CONTAINS, TRANSPORT),
            new CategoryRule(3, "uber eats", MatchType.CONTAINS, FOOD),
            new CategoryRule(4, "swiggy instamart", MatchType.CONTAINS, GROCERIES),
            new CategoryRule(5, "amazon", MatchType.EXACT, SHOPPING),
            new CategoryRule(6, "amazon", MatchType.CONTAINS, SHOPPING)));

    private long categoryOf(String vendor) {
        return categorizer.match(vendor).orElseThrow().categoryId();
    }

    @Test
    void matchesCaseAndPunctuationInsensitively() {
        assertEquals(FOOD, categoryOf("Swiggy"));
        assertEquals(FOOD, categoryOf("SWIGGY*Order 8812"));
        assertEquals(FOOD, categoryOf("  swiggy.  "));
    }

    @Test
    void mostSpecificContainsRuleWins() {
        assertEquals(TRANSPORT, categoryOf("Uber India"));
        assertEquals(FOOD, categoryOf("Uber Eats"));
        assertEquals(GROCERIES, categoryOf("Swiggy Instamart"));
    }

    @Test
    void containsMatchesWholeWordsOnly() {
        assertFalse(categorizer.match("Tuber Farms").isPresent());
        assertFalse(categorizer.match("Swiggyy").isPresent());
    }

    @Test
    void exactRuleTakesPrecedence() {
        assertEquals(5, categorizer.match("AMAZON").orElseThrow().ruleId());
        assertEquals(6, categorizer.match("Amazon Pay").orElseThrow().ruleId());
    }

    @Test
    void unknownOrBlankVendorHasNoMatch() {
        assertFalse(categorizer.match("Local Chaiwala").isPresent());
        assertFalse(categorizer.match("   ").isPresent());
        assertFalse(categorizer.match(null).isPresent());
    }

    @Test
    void normalizerStripsAccentsAndSymbols() {
        assertEquals("cafe coffee day", VendorNormalizer.normalize("Café-Coffee_Day!!"));
        assertTrue(VendorNormalizer.tokens("").isEmpty());
    }
}
