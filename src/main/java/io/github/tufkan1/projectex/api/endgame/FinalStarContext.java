package io.github.tufkan1.projectex.api.endgame;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned lookup context; capabilities never authorize a client-only request. */
public record FinalStarContext(ServerPlayer player, FinalStarSlot slot) {
    public FinalStarContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(slot, "slot");
    }
}
