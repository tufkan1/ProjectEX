package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.network.AlchemyActionPayload;
import io.github.tufkan1.projectex.network.AlchemyResultPayload;
import io.github.tufkan1.projectex.network.AlchemySessionPayload;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/** Client cache that accepts only server-authoritative state for its active session. */
public final class ClientAlchemySessionState {
    private Snapshot snapshot = Snapshot.closed();

    public synchronized boolean open(AlchemySessionPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (!payload.isStructurallyValid()) {
            return false;
        }
        snapshot = new Snapshot(
            true,
            payload.sessionId(),
            payload.emcRevision(),
            payload.parsedBalance().orElseThrow(),
            payload.knowledgeCount(),
            0,
            -1,
            Optional.empty()
        );
        return true;
    }

    public synchronized Optional<AlchemyActionPayload> nextAction(int operationId, String itemId, int count) {
        Objects.requireNonNull(itemId, "itemId");
        if (!snapshot.active() || snapshot.nextRequestId() == Long.MAX_VALUE) {
            return Optional.empty();
        }
        long requestId = snapshot.nextRequestId();
        AlchemyActionPayload payload;
        try {
            payload = new AlchemyActionPayload(
                io.github.tufkan1.projectex.network.AlchemyNetworkProtocol.VERSION,
                snapshot.sessionId(),
                requestId,
                operationId,
                itemId,
                count,
                snapshot.emcRevision()
            );
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        if (payload.toTransaction().isEmpty()) {
            return Optional.empty();
        }
        snapshot = snapshot.withNextRequestId(requestId + 1);
        return Optional.of(payload);
    }

    public synchronized boolean accept(AlchemyResultPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (!snapshot.active()
            || !payload.isStructurallyValid()
            || payload.sessionId() != snapshot.sessionId()
            || payload.requestId() < 0
            || payload.requestId() >= snapshot.nextRequestId()
            || payload.requestId() <= snapshot.lastResponseId()) {
            return false;
        }
        snapshot = new Snapshot(
            true,
            snapshot.sessionId(),
            payload.emcRevision(),
            payload.parsedBalance().orElseThrow(),
            payload.knowledgeCount(),
            snapshot.nextRequestId(),
            payload.requestId(),
            payload.success() ? Optional.empty() : payload.failure()
        );
        return true;
    }

    public synchronized void close() {
        snapshot = Snapshot.closed();
    }

    public synchronized Snapshot snapshot() {
        return snapshot;
    }

    public record Snapshot(
        boolean active,
        long sessionId,
        long emcRevision,
        BigInteger balance,
        int knowledgeCount,
        long nextRequestId,
        long lastResponseId,
        Optional<AlchemyTransactionFailure> lastFailure
    ) {
        public Snapshot {
            Objects.requireNonNull(balance, "balance");
            Objects.requireNonNull(lastFailure, "lastFailure");
        }

        private static Snapshot closed() {
            return new Snapshot(false, 0, 0, BigInteger.ZERO, 0, 0, -1, Optional.empty());
        }

        private Snapshot withNextRequestId(long next) {
            return new Snapshot(active, sessionId, emcRevision, balance, knowledgeCount,
                next, lastResponseId, lastFailure);
        }
    }
}
