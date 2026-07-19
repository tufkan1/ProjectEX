package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tiny bounded keybinding request; the server resolves the held stack and authority. */
public record UtilityStatePayload(int action, int hand) implements CustomPacketPayload {
    public static final Type<UtilityStatePayload> TYPE = new Type<>(ProjectEX.id("utility_state_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UtilityStatePayload> CODEC =
        StreamCodec.ofMember(UtilityStatePayload::write, UtilityStatePayload::read);

    public boolean hasValidShape() {
        return action >= 0 && action < UtilityStateAction.values().length && hand >= 0 && hand < 2;
    }
    public UtilityStateAction resolvedAction() { return UtilityStateAction.values()[action]; }

    @Override public Type<UtilityStatePayload> type() { return TYPE; }
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(action);
        buffer.writeByte(hand);
    }
    private static UtilityStatePayload read(RegistryFriendlyByteBuf buffer) {
        return new UtilityStatePayload(buffer.readByte(), buffer.readByte());
    }
}
