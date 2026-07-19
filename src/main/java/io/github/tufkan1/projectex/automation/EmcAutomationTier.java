package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;

/** Tier budgets shared by EMC Links and Transmutation Interfaces. */
public record EmcAutomationTier(
    ExpansionMachineTier matter,
    EmcValue maximumPerRequest,
    EmcValue maximumPerTick,
    int maximumRequestsPerTick,
    int maximumFilterEntries
) {
    public EmcAutomationTier {
        java.util.Objects.requireNonNull(matter, "matter");
        java.util.Objects.requireNonNull(maximumPerRequest, "maximumPerRequest");
        java.util.Objects.requireNonNull(maximumPerTick, "maximumPerTick");
        if (maximumPerRequest.equals(EmcValue.ZERO)
            || maximumPerTick.compareTo(maximumPerRequest) < 0
            || maximumRequestsPerTick < 1 || maximumFilterEntries < 1) {
            throw new IllegalArgumentException("Invalid EMC automation tier budget");
        }
    }

    public static EmcAutomationTier of(ExpansionMachineTier matter) {
        EmcValue request = matter.relayTransferPerTick();
        return new EmcAutomationTier(
            matter,
            request,
            request.multiply(32),
            Math.min(256, 8 + matter.level() * 4),
            Math.min(64, matter.level() * 4)
        );
    }
}
