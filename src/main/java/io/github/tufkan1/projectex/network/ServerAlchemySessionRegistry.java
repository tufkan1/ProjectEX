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
