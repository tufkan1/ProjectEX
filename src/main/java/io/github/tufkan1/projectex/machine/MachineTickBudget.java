package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;

/** Hard per-tick work and EMC limits used to bound machine networks. */
public record MachineTickBudget(int maxTransfers, EmcValue maxTransferredEmc) {
    public MachineTickBudget {
        Objects.requireNonNull(maxTransferredEmc, "maxTransferredEmc");
        if (maxTransfers < 1 || maxTransferredEmc.equals(EmcValue.ZERO)) {
            throw new IllegalArgumentException("Tick budgets must be positive");
        }
    }

    public static MachineTickBudget baseline(MachineTier tier) {
        return new MachineTickBudget(32, tier.rate().multiply(6));
    }
}
