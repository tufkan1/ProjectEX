package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;

/** Baseline ProjectE machines plus ProjectExpansion post-MK3 machine tiers. */
public enum MachineTier {
    COLLECTOR_MK1(MachineType.COLLECTOR, 1, 4, 10_000),
    COLLECTOR_MK2(MachineType.COLLECTOR, 2, 12, 30_000),
    COLLECTOR_MK3(MachineType.COLLECTOR, 3, 40, 60_000),
    RELAY_MK1(MachineType.RELAY, 1, 64, 100_000),
    RELAY_MK2(MachineType.RELAY, 2, 192, 1_000_000),
    RELAY_MK3(MachineType.RELAY, 3, 640, 10_000_000),

    COLLECTOR_MAGENTA(MachineType.COLLECTOR, ExpansionMachineTier.MAGENTA),
    COLLECTOR_PINK(MachineType.COLLECTOR, ExpansionMachineTier.PINK),
    COLLECTOR_PURPLE(MachineType.COLLECTOR, ExpansionMachineTier.PURPLE),
    COLLECTOR_VIOLET(MachineType.COLLECTOR, ExpansionMachineTier.VIOLET),
    COLLECTOR_BLUE(MachineType.COLLECTOR, ExpansionMachineTier.BLUE),
    COLLECTOR_CYAN(MachineType.COLLECTOR, ExpansionMachineTier.CYAN),
    COLLECTOR_GREEN(MachineType.COLLECTOR, ExpansionMachineTier.GREEN),
    COLLECTOR_LIME(MachineType.COLLECTOR, ExpansionMachineTier.LIME),
    COLLECTOR_YELLOW(MachineType.COLLECTOR, ExpansionMachineTier.YELLOW),
    COLLECTOR_ORANGE(MachineType.COLLECTOR, ExpansionMachineTier.ORANGE),
    COLLECTOR_WHITE(MachineType.COLLECTOR, ExpansionMachineTier.WHITE),
    COLLECTOR_FADING(MachineType.COLLECTOR, ExpansionMachineTier.FADING),
    COLLECTOR_FINAL(MachineType.COLLECTOR, ExpansionMachineTier.FINAL),

    RELAY_MAGENTA(MachineType.RELAY, ExpansionMachineTier.MAGENTA),
    RELAY_PINK(MachineType.RELAY, ExpansionMachineTier.PINK),
    RELAY_PURPLE(MachineType.RELAY, ExpansionMachineTier.PURPLE),
    RELAY_VIOLET(MachineType.RELAY, ExpansionMachineTier.VIOLET),
    RELAY_BLUE(MachineType.RELAY, ExpansionMachineTier.BLUE),
    RELAY_CYAN(MachineType.RELAY, ExpansionMachineTier.CYAN),
    RELAY_GREEN(MachineType.RELAY, ExpansionMachineTier.GREEN),
    RELAY_LIME(MachineType.RELAY, ExpansionMachineTier.LIME),
    RELAY_YELLOW(MachineType.RELAY, ExpansionMachineTier.YELLOW),
    RELAY_ORANGE(MachineType.RELAY, ExpansionMachineTier.ORANGE),
    RELAY_WHITE(MachineType.RELAY, ExpansionMachineTier.WHITE),
    RELAY_FADING(MachineType.RELAY, ExpansionMachineTier.FADING),
    RELAY_FINAL(MachineType.RELAY, ExpansionMachineTier.FINAL),

    POWER_FLOWER_BASIC(MachineType.POWER_FLOWER, ExpansionMachineTier.BASIC),
    POWER_FLOWER_DARK(MachineType.POWER_FLOWER, ExpansionMachineTier.DARK),
    POWER_FLOWER_RED(MachineType.POWER_FLOWER, ExpansionMachineTier.RED),
    POWER_FLOWER_MAGENTA(MachineType.POWER_FLOWER, ExpansionMachineTier.MAGENTA),
    POWER_FLOWER_PINK(MachineType.POWER_FLOWER, ExpansionMachineTier.PINK),
    POWER_FLOWER_PURPLE(MachineType.POWER_FLOWER, ExpansionMachineTier.PURPLE),
    POWER_FLOWER_VIOLET(MachineType.POWER_FLOWER, ExpansionMachineTier.VIOLET),
    POWER_FLOWER_BLUE(MachineType.POWER_FLOWER, ExpansionMachineTier.BLUE),
    POWER_FLOWER_CYAN(MachineType.POWER_FLOWER, ExpansionMachineTier.CYAN),
    POWER_FLOWER_GREEN(MachineType.POWER_FLOWER, ExpansionMachineTier.GREEN),
    POWER_FLOWER_LIME(MachineType.POWER_FLOWER, ExpansionMachineTier.LIME),
    POWER_FLOWER_YELLOW(MachineType.POWER_FLOWER, ExpansionMachineTier.YELLOW),
    POWER_FLOWER_ORANGE(MachineType.POWER_FLOWER, ExpansionMachineTier.ORANGE),
    POWER_FLOWER_WHITE(MachineType.POWER_FLOWER, ExpansionMachineTier.WHITE),
    POWER_FLOWER_FADING(MachineType.POWER_FLOWER, ExpansionMachineTier.FADING),
    POWER_FLOWER_FINAL(MachineType.POWER_FLOWER, ExpansionMachineTier.FINAL);

    private final MachineType type;
    private final int level;
    private final EmcValue rate;
    private final EmcValue capacity;
    private final FixedPointRate fixedRate;
    private final ExpansionMachineTier expansionTier;

    MachineTier(MachineType type, int level, long rate, long capacity) {
        this.type = type;
        this.level = level;
        this.rate = EmcValue.of(rate);
        this.capacity = EmcValue.of(capacity);
        this.fixedRate = FixedPointRate.perTick(rate);
        this.expansionTier = null;
    }

    MachineTier(MachineType type, ExpansionMachineTier expansionTier) {
        this.type = type;
        this.level = expansionTier.level();
        this.expansionTier = expansionTier;
        if (type == MachineType.COLLECTOR) {
            rate = expansionTier.collectorPerSecond();
            capacity = collectorCapacity(expansionTier.collectorPerSecond());
            fixedRate = expansionTier.collectorRate();
        } else if (type == MachineType.RELAY) {
            rate = expansionTier.relayTransferPerTick();
            capacity = new EmcValue(BigInteger.valueOf(Long.MAX_VALUE));
            fixedRate = expansionTier.relayBonusRate();
        } else {
            rate = expansionTier.powerFlowerPerSecond();
            capacity = new EmcValue(BigInteger.valueOf(Long.MAX_VALUE));
            fixedRate = expansionTier.powerFlowerRate();
        }
    }

    public MachineType type() {
        return type;
    }

    public int level() {
        return level;
    }

    public EmcValue rate() {
        return rate;
    }

    public EmcValue capacity() {
        return capacity;
    }

    public FixedPointRate fixedRate() {
        return fixedRate;
    }

    public java.util.Optional<ExpansionMachineTier> expansionTier() {
        return java.util.Optional.ofNullable(expansionTier);
    }

    public static MachineTier expansion(MachineType type, ExpansionMachineTier tier) {
        if (type == MachineType.COLLECTOR && tier.ordinal() < ExpansionMachineTier.MAGENTA.ordinal()) {
            throw new IllegalArgumentException("Basic, dark, and red collectors use baseline MK1-MK3 blocks");
        }
        if (type == MachineType.RELAY && tier.ordinal() < ExpansionMachineTier.MAGENTA.ordinal()) {
            throw new IllegalArgumentException("Basic, dark, and red relays use baseline MK1-MK3 blocks");
        }
        return valueOf(type.name() + "_" + tier.name());
    }

    private static EmcValue collectorCapacity(EmcValue perSecond) {
        BigInteger minute = perSecond.amount().multiply(BigInteger.valueOf(60));
        BigInteger magnitude = BigInteger.TEN.pow(minute.toString().length() - 1);
        BigInteger step = magnitude.divide(BigInteger.valueOf(4));
        if (step.signum() == 0) {
            step = BigInteger.ONE;
        }
        BigInteger[] division = minute.divideAndRemainder(step);
        BigInteger rounded = division[0].add(division[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE)
            .multiply(step);
        return new EmcValue(rounded.min(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    public enum MachineType {
        COLLECTOR,
        RELAY,
        POWER_FLOWER
    }
}
