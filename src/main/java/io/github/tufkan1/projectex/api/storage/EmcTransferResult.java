package io.github.tufkan1.projectex.api.storage;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;

/** Exact accounting result; requested always equals transferred plus remainder. */
public record EmcTransferResult(
    EmcValue requested,
    EmcValue transferred,
    EmcValue remainder,
    EmcValue resultingStored,
    boolean executed,
    boolean allowed
) {
    public EmcTransferResult {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(transferred, "transferred");
        Objects.requireNonNull(remainder, "remainder");
        Objects.requireNonNull(resultingStored, "resultingStored");
        if (!requested.equals(transferred.add(remainder))) {
            throw new IllegalArgumentException("Transfer accounting does not balance");
        }
        if (!allowed && !transferred.equals(EmcValue.ZERO)) {
            throw new IllegalArgumentException("Denied transfer cannot move EMC");
        }
    }

    public boolean complete() {
        return allowed && remainder.equals(EmcValue.ZERO);
    }
}
