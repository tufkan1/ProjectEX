package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.content.ChargeableUtilityItem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Rate-limited server receiver for charge/mode keybindings. */
public final class UtilityNetworking {
    private static final Map<UUID, Long> LAST_TICK = new ConcurrentHashMap<>();

    private UtilityNetworking() { }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(UtilityStatePayload.TYPE, UtilityStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UtilityStatePayload.TYPE, (payload, context) -> {
            if (!payload.hasValidShape()) return;
            long tick = context.player().level().getGameTime();
            Long previous = LAST_TICK.put(context.player().getUUID(), tick);
            if (previous != null && previous == tick) return;
            InteractionHand hand = payload.hand() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = context.player().getItemInHand(hand);
            if (stack.getItem() instanceof ChargeableUtilityItem utility) {
                utility.cycle(stack, context.player(), payload.resolvedAction());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            LAST_TICK.remove(handler.player.getUUID()));
    }
}
