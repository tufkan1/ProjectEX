package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.internal.knowledge.KnowledgeSharingRuntime;
import io.github.tufkan1.projectex.content.component.KnowledgeBookState;
import java.time.Instant;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Payload registration for the explicit knowledge preview/decision protocol. */
public final class KnowledgeSharingNetworking {
    private KnowledgeSharingNetworking() { }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
            KnowledgeSharePreviewPayload.TYPE, KnowledgeSharePreviewPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            KnowledgeShareDecisionPayload.TYPE, KnowledgeShareDecisionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
            KnowledgeShareResultPayload.TYPE, KnowledgeShareResultPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KnowledgeShareDecisionPayload.TYPE, (payload, context) -> {
            KnowledgeShareResultPayload result = KnowledgeSharingRuntime.get(context.server()).decide(
                context.player(), payload.token(), payload.accepted(), Instant.now());
            context.responseSender().sendPacket(result);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            KnowledgeSharingRuntime.get(server).disconnect(handler.player.getUUID()));
    }

    public static boolean preview(ServerPlayer player, KnowledgeBookState book) {
        KnowledgeSharingRuntime.PreviewResponse response = KnowledgeSharingRuntime.get(
            player.level().getServer()).preview(player, book, Instant.now());
        response.preview().ifPresent(payload -> ServerPlayNetworking.send(player, payload));
        response.failure().ifPresent(payload -> ServerPlayNetworking.send(player, payload));
        return response.preview().isPresent();
    }
}
