package io.github.tufkan1.projectex.api.emc;

import java.math.BigInteger;
import java.util.Objects;

/** An arbitrary-precision, non-negative EMC amount. */
public record EmcValue(BigInteger amount) implements Comparable<EmcValue> {
    public static final EmcValue ZERO = new EmcValue(BigInteger.ZERO);

    public EmcValue {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("EMC cannot be negative");
        }
    }

    public static EmcValue of(long amount) {
        return new EmcValue(BigInteger.valueOf(amount));
    }

    public EmcValue add(EmcValue other) {
        return new EmcValue(amount.add(other.amount));
    }

    public EmcValue subtract(EmcValue other) {
        BigInteger result = amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("EMC subtraction cannot produce a negative value");
        }
        return new EmcValue(result);
    }

    public EmcValue min(EmcValue other) {
        return compareTo(other) <= 0 ? this : other;
    }

    public EmcValue multiply(long multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new EmcValue(amount.multiply(BigInteger.valueOf(multiplier)));
    }

    @Override
    public int compareTo(EmcValue other) {
        return amount.compareTo(other.amount);
    }
}
