package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Empty bounded request; the server locates and validates the player's Arcane Tablet. */
public record OpenArcaneTabletPayload() implements CustomPacketPayload {
    public static final OpenArcaneTabletPayload INSTANCE = new OpenArcaneTabletPayload();
    public static final Type<OpenArcaneTabletPayload> TYPE =
        new Type<>(ProjectEX.id("open_arcane_tablet_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenArcaneTabletPayload> CODEC =
        StreamCodec.of((buffer, payload) -> { }, buffer -> INSTANCE);

    @Override public Type<OpenArcaneTabletPayload> type() { return TYPE; }
}
