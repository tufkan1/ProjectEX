package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Objects;

/** Complete post-transaction state, or the untouched pre-transaction state on failure. */
public record AlchemyTransactionResult(
    boolean success,
    AlchemyTransactionFailure failure,
    PlayerAlchemyState player,
    AlchemyInventory inventory,
    EmcValue unitValue
) {
    public AlchemyTransactionResult {
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(unitValue, "unitValue");
        if (success != (failure == AlchemyTransactionFailure.NONE)) {
            throw new IllegalArgumentException("Success and failure reason disagree");
        }
    }

    static AlchemyTransactionResult success(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        EmcValue unitValue
    ) {
        return new AlchemyTransactionResult(true, AlchemyTransactionFailure.NONE, player, inventory, unitValue);
    }

    static AlchemyTransactionResult failure(
        AlchemyTransactionFailure failure,
        PlayerAlchemyState player,
        AlchemyInventory inventory
    ) {
        return new AlchemyTransactionResult(false, failure, player, inventory, EmcValue.ZERO);
    }
}
