package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common payload registration and server receiver wiring. */
public final class AlchemyNetworking {
    private static final ServerAlchemySessionRegistry SESSIONS = new ServerAlchemySessionRegistry();

    private AlchemyNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(AlchemyActionPayload.TYPE, AlchemyActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AlchemyResultPayload.TYPE, AlchemyResultPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AlchemyActionPayload.TYPE, (payload, context) -> {
            PlayerAlchemyState fallback = PlayerAlchemySavedData.get(context.server())
                .state(context.player().getUUID());
            AlchemyResultPayload result = SESSIONS.handle(
                context.player().getUUID(),
                !context.player().hasDisconnected(),
                payload,
                ProjectEX.emc().snapshot(),
                fallback,
                System.nanoTime() / 1_000_000L
            );
            context.responseSender().sendPacket(result);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            SESSIONS.close(handler.player.getUUID()));
    }

    /** Called only by a server-created transmutation menu. */
    public static ServerAlchemySessionRegistry sessions() {
        return SESSIONS;
    }
}
