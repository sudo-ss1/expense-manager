package com.iconcile.expenses.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Canonicalises free-text vendor names so that "SWIGGY*Order 8812", "swiggy" and "Swiggy." compare equal
 * at the token level. Used both for rule matching and for grouping vendors on the dashboard.
 */
public final class VendorNormalizer {

    private VendorNormalizer() {
    }

    /** Lower-cases, strips accents and replaces every run of non letter/digit characters with one space. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return s.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public static List<String> tokens(String raw) {
        String n = normalize(raw);
        return n.isEmpty() ? List.of() : Arrays.asList(n.split(" "));
    }
}
