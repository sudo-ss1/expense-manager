package com.iconcile.expenses.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector(new BigDecimal("3"), 2);

    private static AnomalyDetector.Observation<String> obs(String id, String amount) {
        return new AnomalyDetector.Observation<>(id, new BigDecimal(amount));
    }

    private Map<String, AnomalyDetector.Verdict<String>> run(List<AnomalyDetector.Observation<String>> list) {
        return detector.evaluate(list).stream()
                .collect(Collectors.toMap(AnomalyDetector.Verdict::id, Function.identity()));
    }

    @Test
    void flagsExpenseAboveThreeTimesPeerAverage() {
        var result = run(List.of(obs("a", "200"), obs("b", "300"), obs("c", "250"), obs("big", "2000")));
        assertTrue(result.get("big").anomaly());
        assertEquals(new BigDecimal("250.00"), result.get("big").baseline());
        assertFalse(result.get("a").anomaly());
    }

    @Test
    void outlierDoesNotHideItselfInSmallCategories() {
        // With 3 items a naive mean that includes the outlier can never be exceeded 3x.
        var result = run(List.of(obs("a", "100"), obs("b", "100"), obs("big", "1000")));
        assertTrue(result.get("big").anomaly());
    }

    @Test
    void exactlyThreeTimesIsNotAnAnomaly() {
        var result = run(List.of(obs("a", "100"), obs("b", "100"), obs("x", "300")));
        assertFalse(result.get("x").anomaly(), "rule is 'more than 3x', so exactly 3x is fine");
        var justAbove = run(List.of(obs("a", "100"), obs("b", "100"), obs("x", "300.01")));
        assertTrue(justAbove.get("x").anomaly());
    }

    @Test
    void tooFewPeersMeansNoVerdict() {
        var result = run(List.of(obs("a", "10"), obs("b", "10000")));
        assertFalse(result.get("b").anomaly());
        assertNull(result.get("b").baseline());
    }

    @Test
    void uniformSpendingHasNoAnomalies() {
        var result = run(List.of(obs("a", "500"), obs("b", "520"), obs("c", "480"), obs("d", "510")));
        assertTrue(result.values().stream().noneMatch(AnomalyDetector.Verdict::anomaly));
    }

    @Test
    void readTimeBaselineMatchesEvaluation() {
        // category of 4: 200 + 300 + 250 + 2000 = 2750; baseline for 2000 is (2750 - 2000) / 3 = 250
        assertEquals(new BigDecimal("250.00"), detector.baseline(new BigDecimal("2000"), 4, new BigDecimal("2750")));
        assertNull(detector.baseline(new BigDecimal("10"), 2, new BigDecimal("10010")), "1 peer is below minPeers");
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new AnomalyDetector(BigDecimal.ZERO, 2));
        assertThrows(IllegalArgumentException.class, () -> new AnomalyDetector(BigDecimal.TEN, 0));
    }
}
