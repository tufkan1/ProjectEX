package io.github.tufkan1.projectex.alchemy;

import java.util.UUID;

/** Structured, amount-free transaction audit metadata. */
public record AlchemyAuditEvent(
    UUID playerId,
    String operation,
    boolean success,
    AlchemyTransactionFailure failure,
    long emcRevision
) {
    public AlchemyAuditEvent {
        java.util.Objects.requireNonNull(playerId, "playerId");
        java.util.Objects.requireNonNull(operation, "operation");
        java.util.Objects.requireNonNull(failure, "failure");
    }
}
