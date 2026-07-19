package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.UUID;

/**
 * Server-owned mutable boundary for one player/menu session.
 *
 * <p>{@link #commit} must compare both expected values and replace both new values as
 * one logical operation, returning false without mutation when either expected value changed.</p>
 */
public interface AlchemyTransactionTarget {
    UUID playerId();

    PlayerAlchemyState playerState();

    AlchemyInventory inventory();

    boolean commit(
        PlayerAlchemyState expectedPlayer,
        AlchemyInventory expectedInventory,
        PlayerAlchemyState newPlayer,
        AlchemyInventory newInventory
    );
}
