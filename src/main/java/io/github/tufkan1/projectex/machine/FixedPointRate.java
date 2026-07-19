package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.Objects;

/** Exact rational EMC rate with an explicit persisted remainder. */
public record FixedPointRate(BigInteger numerator, BigInteger denominator) {
    public FixedPointRate {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (numerator.signum() < 0 || denominator.signum() <= 0) {
            throw new IllegalArgumentException("Rate must be non-negative with a positive denominator");
        }
    }

    public static FixedPointRate perTick(long amount) {
        return new FixedPointRate(BigInteger.valueOf(amount), BigInteger.ONE);
    }

    public Generation generate(BigInteger previousRemainder, long ticks, EmcValue limit) {
        Objects.requireNonNull(previousRemainder, "previousRemainder");
        Objects.requireNonNull(limit, "limit");
        if (previousRemainder.signum() < 0) {
            throw new IllegalArgumentException("Deferred generation cannot be negative");
        }
        if (ticks < 0) {
            throw new IllegalArgumentException("Ticks cannot be negative");
        }
        BigInteger raw = previousRemainder.add(numerator.multiply(BigInteger.valueOf(ticks)));
        BigInteger[] division = raw.divideAndRemainder(denominator);
        EmcValue unconstrained = new EmcValue(division[0]);
        EmcValue produced = unconstrained.min(limit);
        // A tick budget may defer whole EMC as well as the fractional remainder.
        BigInteger deferred = unconstrained.subtract(produced).amount().multiply(denominator)
            .add(division[1]);
        return new Generation(produced, deferred);
    }

    public record Generation(EmcValue produced, BigInteger deferredNumerator) {
        public Generation {
            Objects.requireNonNull(produced, "produced");
            Objects.requireNonNull(deferredNumerator, "deferredNumerator");
            if (deferredNumerator.signum() < 0) {
                throw new IllegalArgumentException("Deferred generation cannot be negative");
            }
        }
    }
}
