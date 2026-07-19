package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.Objects;

/** Versioned persistent machine state shared by collector and relay block entities. */
public record MachineState(
    int version,
    MachineTier tier,
    EmcValue stored,
    BigInteger deferredGeneration,
    MachineAccess access,
    MachineRedstoneMode redstoneMode
) {
    public static final int CURRENT_VERSION = 1;

    public MachineState {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(stored, "stored");
        Objects.requireNonNull(deferredGeneration, "deferredGeneration");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(redstoneMode, "redstoneMode");
        if (version != CURRENT_VERSION || stored.compareTo(tier.capacity()) > 0
            || deferredGeneration.signum() < 0) {
            throw new IllegalArgumentException("Invalid machine state");
        }
    }

    public static MachineState empty(MachineTier tier) {
        return new MachineState(
            CURRENT_VERSION,
            tier,
            EmcValue.ZERO,
            BigInteger.ZERO,
            MachineAccess.UNCLAIMED,
            MachineRedstoneMode.IGNORED
        );
    }
}
