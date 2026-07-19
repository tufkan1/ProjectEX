package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Objects;
import java.util.Optional;

/** Pure server-authoritative learn, burn, and create transaction evaluator. */
public final class AlchemyTransactionService {
    public static final int MAX_REQUEST_COUNT = 64;

    private AlchemyTransactionService() {
    }

    public static AlchemyTransactionResult execute(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request,
        EmcSnapshot emc
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(emc, "emc");
        if (request.emcRevision() != emc.revision()) {
            return failure(AlchemyTransactionFailure.STALE_EMC_REVISION, player, inventory);
        }
        Optional<EmcValue> unitValue = emc.find(request.item());
        if (unitValue.isEmpty() || unitValue.orElseThrow().equals(EmcValue.ZERO)) {
            return failure(AlchemyTransactionFailure.UNKNOWN_EMC_VALUE, player, inventory);
        }
        if (request instanceof AlchemyTransaction.Learn) {
            return learn(player, inventory, request, unitValue.orElseThrow());
        }
        int count = request instanceof AlchemyTransaction.Burn burn ? burn.count()
            : ((AlchemyTransaction.Create) request).count();
        if (count <= 0 || count > MAX_REQUEST_COUNT) {
            return failure(AlchemyTransactionFailure.INVALID_COUNT, player, inventory);
        }
        return request instanceof AlchemyTransaction.Burn
            ? burn(player, inventory, request, count, unitValue.orElseThrow())
            : create(player, inventory, request, count, unitValue.orElseThrow());
    }

    private static AlchemyTransactionResult learn(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request,
        EmcValue unitValue
    ) {
        if (inventory.count(request.item()) < 1) {
            return failure(AlchemyTransactionFailure.ITEM_NOT_PRESENT, player, inventory);
        }
        return AlchemyTransactionResult.success(player.learn(request.item().item()), inventory, unitValue);
    }

    private static AlchemyTransactionResult burn(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request,
        int count,
        EmcValue unitValue
    ) {
        Optional<AlchemyInventory> changedInventory = inventory.remove(request.item(), count);
        if (changedInventory.isEmpty()) {
            return failure(AlchemyTransactionFailure.ITEM_NOT_PRESENT, player, inventory);
        }
        try {
            PlayerAlchemyState changedPlayer = player
                .credit(unitValue.multiply(count))
                .learn(request.item().item());
            return AlchemyTransactionResult.success(
                changedPlayer, changedInventory.orElseThrow(), unitValue);
        } catch (IllegalArgumentException exception) {
            return failure(AlchemyTransactionFailure.BALANCE_LIMIT_EXCEEDED, player, inventory);
        }
    }

    private static AlchemyTransactionResult create(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request,
        int count,
        EmcValue unitValue
    ) {
        if (!player.knows(request.item().item())) {
            return failure(AlchemyTransactionFailure.ITEM_NOT_LEARNED, player, inventory);
        }
        EmcValue total = unitValue.multiply(count);
        Optional<PlayerAlchemyState> changedPlayer = player.debit(total);
        if (changedPlayer.isEmpty()) {
            return failure(AlchemyTransactionFailure.INSUFFICIENT_EMC, player, inventory);
        }
        Optional<AlchemyInventory> changedInventory = inventory.add(request.item(), count);
        if (changedInventory.isEmpty()) {
            return failure(AlchemyTransactionFailure.INVENTORY_FULL, player, inventory);
        }
        return AlchemyTransactionResult.success(
            changedPlayer.orElseThrow(), changedInventory.orElseThrow(), unitValue);
    }

    private static AlchemyTransactionResult failure(
        AlchemyTransactionFailure reason,
        PlayerAlchemyState player,
        AlchemyInventory inventory
    ) {
        return AlchemyTransactionResult.failure(reason, player, inventory);
    }
}
