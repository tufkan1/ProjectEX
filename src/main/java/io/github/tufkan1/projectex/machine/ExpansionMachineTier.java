package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;

/** Exact ProjectExpansion-compatible balance catalog for post-MK3 EMC machines. */
public enum ExpansionMachineTier {
    BASIC, DARK, RED, MAGENTA, PINK, PURPLE, VIOLET, BLUE,
    CYAN, GREEN, LIME, YELLOW, ORANGE, WHITE, FADING, FINAL;

    public static final int POWER_FLOWER_COLLECTORS = 18;
    public static final int POWER_FLOWER_RELAYS = 30;
    private static final BigInteger SCALE = BigInteger.valueOf(6);
    private static final BigInteger TICKS_PER_SECOND = BigInteger.valueOf(20);
    private static final BigInteger BASE_COLLECTOR = BigInteger.valueOf(4);
    private static final BigInteger BASE_RELAY_BONUS = BigInteger.ONE;
    private static final BigInteger BASE_RELAY_TRANSFER = BigInteger.valueOf(64);

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int level() {
        return ordinal() + 1;
    }

    public EmcValue collectorPerSecond() {
        return value(BASE_COLLECTOR.multiply(scale()));
    }

    public FixedPointRate collectorRate() {
        return perSecond(collectorPerSecond());
    }

    public EmcValue relayBonusPerSecond() {
        return value(BASE_RELAY_BONUS.multiply(scale()));
    }

    public FixedPointRate relayBonusRate() {
        return perSecond(relayBonusPerSecond());
    }

    public EmcValue relayTransferPerTick() {
        return this == FINAL
            ? value(BigInteger.valueOf(Long.MAX_VALUE))
            : value(BASE_RELAY_TRANSFER.multiply(scale()));
    }

    public EmcValue powerFlowerPerSecond() {
        BigInteger collectors = collectorPerSecond().amount()
            .multiply(BigInteger.valueOf(POWER_FLOWER_COLLECTORS));
        BigInteger relays = relayBonusPerSecond().amount()
            .multiply(BigInteger.valueOf(POWER_FLOWER_RELAYS));
        return value(collectors.add(relays));
    }

    public FixedPointRate powerFlowerRate() {
        return perSecond(powerFlowerPerSecond());
    }

    private static FixedPointRate perSecond(EmcValue amount) {
        Objects.requireNonNull(amount, "amount");
        return new FixedPointRate(amount.amount(), TICKS_PER_SECOND);
    }

    private static EmcValue value(BigInteger amount) {
        return new EmcValue(amount);
    }

    private BigInteger scale() {
        return SCALE.pow(ordinal());
    }
}
