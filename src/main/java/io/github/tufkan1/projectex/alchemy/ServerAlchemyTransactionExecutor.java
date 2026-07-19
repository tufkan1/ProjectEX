package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Coordinates validation, pure evaluation, atomic compare-and-commit, and auditing. */
public final class ServerAlchemyTransactionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerAlchemyTransactionExecutor.class);
    private final AlchemyTransactionCoordinator coordinator;
    private final AlchemyAuditSink auditSink;

    public ServerAlchemyTransactionExecutor(AlchemyRequestGuard guard, AlchemyAuditSink auditSink) {
        coordinator = new AlchemyTransactionCoordinator(guard);
        this.auditSink = Objects.requireNonNull(auditSink, "auditSink");
    }

    public AlchemyTransactionResult execute(
        AlchemyRequestContext context,
        AlchemyTransactionTarget target,
        AlchemyTransaction request,
        EmcSnapshot emc
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(emc, "emc");
        PlayerAlchemyState player = target.playerState();
        AlchemyInventory inventory = target.inventoryFor(request);
        AlchemyTransactionResult result;
        if (!target.playerId().equals(context.playerId())) {
            result = AlchemyTransactionResult.failure(
                AlchemyTransactionFailure.SESSION_INVALID, player, inventory);
        } else {
            result = coordinator.execute(context, player, inventory, request, emc);
        }
        if (result.success() && !target.commit(
            player, inventory, result.player(), result.inventory())) {
            result = AlchemyTransactionResult.failure(
                AlchemyTransactionFailure.STATE_CHANGED, player, inventory);
        }
        recordAudit(new AlchemyAuditEvent(
            context.playerId(),
            request.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT),
            result.success(),
            result.failure(),
            emc.revision()
        ));
        return result;
    }

    private void recordAudit(AlchemyAuditEvent event) {
        try {
            auditSink.record(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Alchemy audit sink failed for {}", event.playerId(), exception);
        }
    }
}
