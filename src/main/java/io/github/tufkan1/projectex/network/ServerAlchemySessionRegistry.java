package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.alchemy.AlchemyAuditEvent;
import io.github.tufkan1.projectex.alchemy.AlchemyRequestContext;
import io.github.tufkan1.projectex.alchemy.AlchemyRequestGuard;
import io.github.tufkan1.projectex.alchemy.AlchemyTransaction;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionResult;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionTarget;
import io.github.tufkan1.projectex.alchemy.ServerAlchemyTransactionExecutor;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.Locale;

/** Active server-created menu sessions. Client packets cannot create or modify sessions. */
public final class ServerAlchemySessionRegistry {
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final AlchemyRequestGuard guard = new AlchemyRequestGuard();
    private final NetworkRequestLimiter wireLimiter = new NetworkRequestLimiter();
    private final ServerAlchemyTransactionExecutor executor = new ServerAlchemyTransactionExecutor(
        guard,
        ServerAlchemySessionRegistry::audit
    );

    public synchronized SessionHandle open(
        AlchemyTransactionTarget target,
        BooleanSupplier authorizedMenu,
        DoubleSupplier distanceSquared
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(authorizedMenu, "authorizedMenu");
        Objects.requireNonNull(distanceSquared, "distanceSquared");
        long sessionId;
        do {
            sessionId = random.nextLong();
        } while (sessionId == 0);
        sessions.put(target.playerId(), new Session(sessionId, target, authorizedMenu, distanceSquared));
        guard.disconnect(target.playerId());
        wireLimiter.disconnect(target.playerId());
        return new SessionHandle(target.playerId(), sessionId);
    }

    public synchronized AlchemyResultPayload handle(
        UUID playerId,
        boolean connected,
        AlchemyActionPayload payload,
        EmcSnapshot emc,
        PlayerAlchemyState fallbackState,
        long monotonicMillis
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(emc, "emc");
        Objects.requireNonNull(fallbackState, "fallbackState");
        if (!wireLimiter.allow(playerId, monotonicMillis)) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.RATE_LIMITED, fallbackState);
        }
        if (payload.protocolVersion() != AlchemyNetworkProtocol.VERSION) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.UNSUPPORTED_PROTOCOL, fallbackState);
        }
        Session session = sessions.get(playerId);
        if (session == null || session.id != payload.sessionId()) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.SESSION_INVALID, fallbackState);
        }
        if (payload.requestId() < 0) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.MALFORMED_REQUEST,
                session.target.playerState());
        }
        if (payload.requestId() <= session.lastRequestId) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.REPLAYED_REQUEST,
                session.target.playerState());
        }
        session.lastRequestId = payload.requestId();
        Optional<AlchemyTransaction> transaction = payload.toTransaction();
        if (transaction.isEmpty()) {
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.MALFORMED_REQUEST,
                session.target.playerState());
        }
        AlchemyRequestContext context;
        try {
            context = new AlchemyRequestContext(
                playerId,
                connected,
                session.authorizedMenu.getAsBoolean(),
                session.distanceSquared.getAsDouble(),
                monotonicMillis
            );
        } catch (RuntimeException exception) {
            ProjectEX.LOGGER.error("Transmutation session access check failed for {}", playerId, exception);
            sessions.remove(playerId);
            guard.disconnect(playerId);
            return rejected(playerId, payload, emc, AlchemyTransactionFailure.SESSION_INVALID,
                session.target.playerState());
        }
        AlchemyTransactionResult result = executor.execute(context, session.target, transaction.orElseThrow(), emc);
        return AlchemyResultPayload.from(payload.sessionId(), payload.requestId(), emc.revision(), result);
    }

    public synchronized AlchemyKnowledgePagePayload handleKnowledge(
        UUID playerId,
        boolean connected,
        AlchemyKnowledgeRequestPayload payload,
        EmcSnapshot emc,
        long monotonicMillis
    ) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(emc, "emc");
        if (!wireLimiter.allow(playerId, monotonicMillis)) {
            return knowledgeFailure(payload, AlchemyTransactionFailure.RATE_LIMITED);
        }
        if (payload.protocolVersion() != AlchemyNetworkProtocol.VERSION) {
            return knowledgeFailure(payload, AlchemyTransactionFailure.UNSUPPORTED_PROTOCOL);
        }
        Session session = sessions.get(playerId);
        if (session == null || session.id != payload.sessionId()) {
            return knowledgeFailure(payload, AlchemyTransactionFailure.SESSION_INVALID);
        }
        if (!payload.hasValidShape()) {
            return knowledgeFailure(payload, AlchemyTransactionFailure.MALFORMED_REQUEST);
        }
        if (payload.queryId() <= session.lastKnowledgeQueryId) {
            return knowledgeFailure(payload, AlchemyTransactionFailure.REPLAYED_REQUEST);
        }
        session.lastKnowledgeQueryId = payload.queryId();
        try {
            double distance = session.distanceSquared.getAsDouble();
            if (!connected || !session.authorizedMenu.getAsBoolean()) {
                return knowledgeFailure(payload, AlchemyTransactionFailure.SESSION_INVALID);
            }
            if (!Double.isFinite(distance) || distance < 0 || distance > AlchemyRequestGuard.MAX_DISTANCE_SQUARED) {
                return knowledgeFailure(payload, AlchemyTransactionFailure.TOO_FAR);
            }
        } catch (RuntimeException exception) {
            ProjectEX.LOGGER.error("Transmutation knowledge access check failed for {}", playerId, exception);
            close(playerId);
            return knowledgeFailure(payload, AlchemyTransactionFailure.SESSION_INVALID);
        }

        String query = payload.query().strip().toLowerCase(Locale.ROOT);
        java.util.List<AlchemyKnowledgePagePayload.Entry> matching = session.target.playerState().knowledge().stream()
            .filter(item -> query.isEmpty() || item.toString().contains(query))
            .map(item -> emc.find(item)
                .filter(value -> !value.equals(io.github.tufkan1.projectex.api.emc.EmcValue.ZERO))
                .map(value -> new AlchemyKnowledgePagePayload.Entry(
                    item.toString(), value.amount().toString())))
            .flatMap(Optional::stream)
            .toList();
        int totalEntries = matching.size();
        int totalPages = totalEntries == 0 ? 0
            : (totalEntries + payload.pageSize() - 1) / payload.pageSize();
        int page = totalPages == 0 ? 0 : Math.min(payload.page(), totalPages - 1);
        int from = Math.min(totalEntries, page * payload.pageSize());
        int to = Math.min(totalEntries, from + payload.pageSize());
        return new AlchemyKnowledgePagePayload(
            AlchemyNetworkProtocol.VERSION,
            payload.sessionId(),
            payload.queryId(),
            AlchemyTransactionFailure.NONE.ordinal(),
            page,
            totalPages,
            totalEntries,
            matching.subList(from, to)
        );
    }

    public synchronized void close(UUID playerId) {
        sessions.remove(playerId);
        guard.disconnect(playerId);
        wireLimiter.disconnect(playerId);
    }

    public synchronized boolean isOpen(SessionHandle handle) {
        Session session = sessions.get(handle.playerId());
        return session != null && session.id == handle.sessionId();
    }

    private static AlchemyResultPayload rejected(
        UUID playerId,
        AlchemyActionPayload payload,
        EmcSnapshot emc,
        AlchemyTransactionFailure failure,
        PlayerAlchemyState player
    ) {
        audit(new AlchemyAuditEvent(
            playerId,
            "network_reject",
            false,
            failure,
            emc.revision()
        ));
        return AlchemyResultPayload.rejected(
            payload.sessionId(), payload.requestId(), emc.revision(), failure, player);
    }

    private static AlchemyKnowledgePagePayload knowledgeFailure(
        AlchemyKnowledgeRequestPayload payload,
        AlchemyTransactionFailure failure
    ) {
        return new AlchemyKnowledgePagePayload(
            AlchemyNetworkProtocol.VERSION,
            payload.sessionId(),
            Math.max(0, payload.queryId()),
            failure.ordinal(),
            0,
            0,
            0,
            java.util.List.of()
        );
    }

    private static void audit(AlchemyAuditEvent event) {
        if (event.success()) {
            ProjectEX.LOGGER.debug(
                "Alchemy transaction player={} operation={} revision={}",
                event.playerId(), event.operation(), event.emcRevision()
            );
        } else {
            ProjectEX.LOGGER.warn(
                "Rejected alchemy transaction player={} operation={} failure={} revision={}",
                event.playerId(), event.operation(), event.failure(), event.emcRevision()
            );
        }
    }

    private static final class Session {
        private final long id;
        private final AlchemyTransactionTarget target;
        private final BooleanSupplier authorizedMenu;
        private final DoubleSupplier distanceSquared;
        private long lastRequestId = -1;
        private long lastKnowledgeQueryId = -1;

        private Session(
            long id,
            AlchemyTransactionTarget target,
            BooleanSupplier authorizedMenu,
            DoubleSupplier distanceSquared
        ) {
            this.id = id;
            this.target = target;
            this.authorizedMenu = authorizedMenu;
            this.distanceSquared = distanceSquared;
        }
    }

    public record SessionHandle(UUID playerId, long sessionId) {
        public SessionHandle {
            Objects.requireNonNull(playerId, "playerId");
            if (sessionId == 0) {
                throw new IllegalArgumentException("Session id cannot be zero");
            }
        }
    }
}
