package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;

/** Baseline ProjectE-compatible collector and relay balance values. */
public enum MachineTier {
    COLLECTOR_MK1(MachineType.COLLECTOR, 1, 4, 10_000),
    COLLECTOR_MK2(MachineType.COLLECTOR, 2, 12, 30_000),
    COLLECTOR_MK3(MachineType.COLLECTOR, 3, 40, 60_000),
    RELAY_MK1(MachineType.RELAY, 1, 64, 100_000),
    RELAY_MK2(MachineType.RELAY, 2, 192, 1_000_000),
    RELAY_MK3(MachineType.RELAY, 3, 640, 10_000_000);

    private final MachineType type;
    private final int level;
    private final EmcValue rate;
    private final EmcValue capacity;

    MachineTier(MachineType type, int level, long rate, long capacity) {
        this.type = type;
        this.level = level;
        this.rate = EmcValue.of(rate);
        this.capacity = EmcValue.of(capacity);
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
        return FixedPointRate.perTick(rate.amount().longValueExact());
    }

    public enum MachineType {
        COLLECTOR,
        RELAY
    }
}
