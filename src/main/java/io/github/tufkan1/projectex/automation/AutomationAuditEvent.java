package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Optional;
import java.util.UUID;

/** Immutable audit record for every accepted or rejected automation request. */
public record AutomationAuditEvent(
    UUID requestId,
    long tick,
    AutomationOperation operation,
    Optional<UUID> actor,
    Optional<EmcKey> item,
    EmcValue requested,
    EmcValue transferred,
    EmcValue balanceBefore,
    EmcValue balanceAfter,
    Failure failure
) {
    public enum Failure {
        NONE,
        UNAUTHORIZED,
        FILTERED,
        STALE_TICK,
        REQUEST_LIMIT,
        TICK_BUDGET,
        REPLAYED,
        INSUFFICIENT_EMC,
        BALANCE_LIMIT,
        STALE_EMC_REVISION,
        UNKNOWN_ITEM,
        OUTPUT_REJECTED,
        CONTENTION
    }
}
