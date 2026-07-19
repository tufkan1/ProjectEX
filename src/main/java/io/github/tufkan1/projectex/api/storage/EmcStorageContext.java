package io.github.tufkan1.projectex.api.storage;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server authority and automation intent supplied to the item capability lookup. */
public record EmcStorageContext(
    ServerLevel level,
    Optional<ServerPlayer> actor,
    boolean automation
) {
    public EmcStorageContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");
    }

    public static EmcStorageContext player(ServerPlayer player) {
        return new EmcStorageContext(player.level(), Optional.of(player), false);
    }

    public static EmcStorageContext automation(ServerLevel level) {
        return new EmcStorageContext(level, Optional.empty(), true);
    }
}
