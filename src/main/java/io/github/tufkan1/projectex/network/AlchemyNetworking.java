package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionTarget;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Common payload registration and server receiver wiring. */
public final class AlchemyNetworking {
    private static final ServerAlchemySessionRegistry SESSIONS = new ServerAlchemySessionRegistry();

    private AlchemyNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(AlchemyActionPayload.TYPE, AlchemyActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AlchemyResultPayload.TYPE, AlchemyResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AlchemySessionPayload.TYPE, AlchemySessionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            AlchemyKnowledgeRequestPayload.TYPE, AlchemyKnowledgeRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
            AlchemyKnowledgePagePayload.TYPE, AlchemyKnowledgePagePayload.CODEC);
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
        ServerPlayNetworking.registerGlobalReceiver(AlchemyKnowledgeRequestPayload.TYPE, (payload, context) -> {
            AlchemyKnowledgePagePayload result = SESSIONS.handleKnowledge(
                context.player().getUUID(),
                !context.player().hasDisconnected(),
                payload,
                ProjectEX.emc().snapshot(),
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

    public static ServerAlchemySessionRegistry.SessionHandle openSession(
        ServerPlayer player,
        AlchemyTransactionTarget target,
        BooleanSupplier authorizedMenu,
        DoubleSupplier distanceSquared
    ) {
        if (!player.getUUID().equals(target.playerId())) {
            throw new IllegalArgumentException("Player and transaction target identities differ");
        }
        ServerAlchemySessionRegistry.SessionHandle handle = SESSIONS.open(
            target, authorizedMenu, distanceSquared);
        PlayerAlchemyState state = PlayerAlchemySavedData.get(player.level().getServer()).state(player.getUUID());
        ServerPlayNetworking.send(player, AlchemySessionPayload.create(
            handle.sessionId(), ProjectEX.emc().snapshot().revision(), state));
        return handle;
    }

    public static void closeSession(ServerPlayer player) {
        SESSIONS.close(player.getUUID());
    }
}
