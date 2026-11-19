package com.iconcile.expenses.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Flags an expense as anomalous when its amount is strictly greater than {@code multiplier} × the average
 * amount of the <em>other</em> expenses in the same category (leave-one-out mean).
 *
 * <p>Why leave-one-out: if the expense under test is included in its own average, a single large outlier
 * inflates the baseline and hides itself. With n expenses including itself, x &gt; 3·mean requires
 * x &gt; 3·(S)/n, which is mathematically impossible for n ≤ 3. Excluding it gives a stable baseline.
 *
 * <p>An expense is only evaluated when the category has at least {@code minPeers} other expenses; below that
 * the "average" is not meaningful and nothing is flagged.
 *
 * <p>Comparison is done in exact arithmetic: amount·(n−1) &gt; multiplier·(sum − amount), so no rounding
 * error can flip a borderline case.
 */
public final class AnomalyDetector {

    private final BigDecimal multiplier;
    private final int minPeers;

    public AnomalyDetector(BigDecimal multiplier, int minPeers) {
        if (multiplier == null || multiplier.signum() <= 0) {
            throw new IllegalArgumentException("multiplier must be positive");
        }
        if (minPeers < 1) {
            throw new IllegalArgumentException("minPeers must be >= 1");
        }
        this.multiplier = multiplier;
        this.minPeers = minPeers;
    }

    public record Observation<K>(K id, BigDecimal amount) {
    }

    /**
     * @param baseline average of the other expenses in the category (null when there were too few peers)
     */
    public record Verdict<K>(K id, boolean anomaly, BigDecimal baseline) {
    }

    /** Evaluates every expense of ONE category. O(n). */
    public <K> List<Verdict<K>> evaluate(List<Observation<K>> categoryExpenses) {
        int n = categoryExpenses.size();
        BigDecimal sum = categoryExpenses.stream()
                .map(Observation::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Verdict<K>> verdicts = new ArrayList<>(n);
        int peers = n - 1;
        for (Observation<K> o : categoryExpenses) {
            if (peers < minPeers) {
                verdicts.add(new Verdict<>(o.id(), false, null));
                continue;
            }
            BigDecimal othersSum = sum.subtract(o.amount());
            BigDecimal peerCount = BigDecimal.valueOf(peers);
            boolean anomaly = o.amount().multiply(peerCount).compareTo(multiplier.multiply(othersSum)) > 0;
            verdicts.add(new Verdict<>(o.id(), anomaly, baseline(o.amount(), n, sum)));
        }
        return verdicts;
    }

    /**
     * Leave-one-out average for one expense, given its category's current count and sum - used to explain a flag
     * at read time without storing a per-row average that every write would have to rewrite.
     *
     * @return average of the other expenses, or null when the category has fewer than {@code minPeers} others
     */
    public BigDecimal baseline(BigDecimal amount, long categoryCount, BigDecimal categorySum) {
        long peers = categoryCount - 1;
        if (peers < minPeers) {
            return null;
        }
        return categorySum.subtract(amount).divide(BigDecimal.valueOf(peers), 2, RoundingMode.HALF_UP);
    }
}
