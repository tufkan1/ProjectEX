package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Canonical positive rational multiplier for exact machine-rate configuration. */
public record MachineRateMultiplier(BigInteger numerator, BigInteger denominator) {
    private static final BigInteger MAX = BigInteger.valueOf(1_000_000);
    private static final BigInteger MIN_DENOMINATOR = BigInteger.valueOf(1_000);
    public static final MachineRateMultiplier ONE = new MachineRateMultiplier(
        BigInteger.ONE, BigInteger.ONE
    );

    public MachineRateMultiplier {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (numerator.signum() <= 0 || denominator.signum() <= 0) {
            throw new IllegalArgumentException("Machine rate multiplier must be positive");
        }
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
        if (numerator.compareTo(MAX.multiply(denominator)) > 0
            || numerator.multiply(MIN_DENOMINATOR).compareTo(denominator) < 0) {
            throw new IllegalArgumentException("Machine rate multiplier must be between 0.001 and 1000000");
        }
    }

    public static MachineRateMultiplier parse(String value) {
        if (value == null || value.isBlank()) {
            return ONE;
        }
        String input = value.trim();
        if (input.length() > 64) {
            throw new IllegalArgumentException("Machine rate multiplier is too long");
        }
        try {
            int separator = input.indexOf('/');
            if (separator >= 0) {
                if (separator != input.lastIndexOf('/')) {
                    throw new IllegalArgumentException("Invalid machine rate multiplier");
                }
                return new MachineRateMultiplier(
                    decimalInteger(input.substring(0, separator)),
                    decimalInteger(input.substring(separator + 1))
                );
            }
            if (!input.matches("[0-9]+(?:\\.[0-9]+)?")) {
                throw new IllegalArgumentException("Invalid machine rate multiplier");
            }
            BigDecimal decimal = new BigDecimal(input);
            return new MachineRateMultiplier(
                decimal.unscaledValue(), BigInteger.TEN.pow(Math.max(0, decimal.scale()))
            );
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Invalid machine rate multiplier", exception);
        }
    }

    public FixedPointRate apply(FixedPointRate rate) {
        Objects.requireNonNull(rate, "rate");
        return new FixedPointRate(
            rate.numerator().multiply(numerator),
            rate.denominator().multiply(denominator)
        );
    }

    public EmcValue applyFloor(EmcValue value) {
        Objects.requireNonNull(value, "value");
        return new EmcValue(value.amount().multiply(numerator).divide(denominator));
    }

    private static BigInteger decimalInteger(String value) {
        if (value.isEmpty() || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid machine rate multiplier");
        }
        return new BigInteger(value);
    }
}
