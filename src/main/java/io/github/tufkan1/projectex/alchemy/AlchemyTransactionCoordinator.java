package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Optional;

/** Applies server-computed access/rate checks before the pure atomic evaluator. */
public final class AlchemyTransactionCoordinator {
    private final AlchemyRequestGuard guard;

    public AlchemyTransactionCoordinator(AlchemyRequestGuard guard) {
        this.guard = java.util.Objects.requireNonNull(guard, "guard");
    }

    public AlchemyTransactionResult execute(
        AlchemyRequestContext context,
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request,
        EmcSnapshot emc
    ) {
        Optional<AlchemyTransactionFailure> accessFailure = guard.validate(context);
        return accessFailure
            .map(reason -> AlchemyTransactionResult.failure(reason, player, inventory))
            .orElseGet(() -> AlchemyTransactionService.execute(player, inventory, request, emc));
    }
}
