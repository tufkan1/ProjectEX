package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.teleport.AlchemicalBookTier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Common registration for the Alchemical Book session protocol. */
public final class AlchemicalBookNetworking {
    private static final AlchemicalBookSessionRegistry SESSIONS = new AlchemicalBookSessionRegistry();
    private AlchemicalBookNetworking() { }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(
            AlchemicalBookActionPayload.TYPE, AlchemicalBookActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
            AlchemicalBookViewPayload.TYPE, AlchemicalBookViewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AlchemicalBookActionPayload.TYPE, (payload, context) ->
            context.responseSender().sendPacket(SESSIONS.handle(
                context.player(), payload, System.nanoTime() / 1_000_000L)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            SESSIONS.close(handler.player.getUUID()));
    }

    public static void open(
        ServerPlayer player, InteractionHand hand, ItemStack stack, AlchemicalBookTier tier
    ) {
        ServerPlayNetworking.send(player, SESSIONS.open(player, hand, stack, tier));
    }

    public static AlchemicalBookSessionRegistry sessions() { return SESSIONS; }
}
