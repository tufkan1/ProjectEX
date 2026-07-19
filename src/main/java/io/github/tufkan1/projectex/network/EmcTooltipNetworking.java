package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Publishes tooltip data after the authoritative EMC registry has been atomically replaced. */
public final class EmcTooltipNetworking {
    private static volatile MinecraftServer activeServer;

    private EmcTooltipNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(EmcTooltipSyncPayload.TYPE, EmcTooltipSyncPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> activeServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (activeServer == server) activeServer = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            send(handler.getPlayer(), ProjectEX.emc().snapshot()));
        ProjectEX.emc().subscribe(snapshot -> {
            MinecraftServer server = activeServer;
            if (server != null) server.execute(() -> PlayerLookup.all(server).forEach(player -> send(player, snapshot)));
        });
    }

    private static void send(ServerPlayer player, EmcSnapshot snapshot) {
        List<EmcTooltipSyncPayload.Entry> entries = snapshot.values().entrySet().stream()
            .filter(entry -> entry.getKey().componentsJson() == null)
            .filter(entry -> entry.getValue().amount().signum() > 0)
            .limit(EmcTooltipSyncPayload.MAX_ENTRIES)
            .map(entry -> new EmcTooltipSyncPayload.Entry(
                entry.getKey().item().toString(), entry.getValue().amount().toString()))
            .toList();
        ServerPlayNetworking.send(player, new EmcTooltipSyncPayload(
            AlchemyNetworkProtocol.VERSION, snapshot.revision(), entries));
    }
}
