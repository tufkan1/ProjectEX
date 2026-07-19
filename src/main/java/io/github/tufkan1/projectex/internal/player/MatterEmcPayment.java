package io.github.tufkan1.projectex.internal.player;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import net.minecraft.server.level.ServerPlayer;

/** Exact compare-and-set EMC payment boundary for matter actions. */
public final class MatterEmcPayment {
    private MatterEmcPayment() {
    }

    public static EmcValue balance(ServerPlayer player) {
        return data(player).state(player.getUUID()).balance();
    }

    public static boolean debit(ServerPlayer player, EmcValue amount) {
        PlayerAlchemySavedData data = data(player);
        var before = data.state(player.getUUID());
        return before.debit(amount)
            .map(after -> data.compareAndSet(player.getUUID(), before, after))
            .orElse(false);
    }

    public static void credit(ServerPlayer player, EmcValue amount) {
        data(player).update(player.getUUID(), state -> state.credit(amount));
    }

    private static PlayerAlchemySavedData data(ServerPlayer player) {
        return PlayerAlchemySavedData.get(player.level().getServer());
    }
}
