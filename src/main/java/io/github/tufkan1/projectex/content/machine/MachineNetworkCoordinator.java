package io.github.tufkan1.projectex.content.machine;

import io.github.tufkan1.projectex.machine.MachineNetworkTick;
import io.github.tufkan1.projectex.machine.MachineRuntimeConfig;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/** Shares one conservative route ledger between all machines in a server-level tick. */
final class MachineNetworkCoordinator {
    private static final Map<ServerLevel, LevelTick> LEVELS = new WeakHashMap<>();

    private MachineNetworkCoordinator() {
    }

    static synchronized MachineNetworkTick current(ServerLevel level) {
        long gameTime = level.getGameTime();
        LevelTick current = LEVELS.get(level);
        if (current == null || current.gameTime != gameTime) {
            current = new LevelTick(gameTime, new MachineNetworkTick(MachineRuntimeConfig.networkBudget()));
            LEVELS.put(level, current);
        }
        return current.tick;
    }

    private record LevelTick(long gameTime, MachineNetworkTick tick) {
    }
}
