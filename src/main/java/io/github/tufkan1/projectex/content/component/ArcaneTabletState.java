package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned mode carried by the Arcane Tablet item stack. */
public record ArcaneTabletState(int version, Mode mode) {
    public static final int CURRENT_VERSION = 1;
    public static final ArcaneTabletState DEFAULT = new ArcaneTabletState(CURRENT_VERSION, Mode.TRANSMUTATION);
    public static final Codec<ArcaneTabletState> CODEC = Codec.STRING.xmap(
        value -> new ArcaneTabletState(CURRENT_VERSION, Mode.valueOf(value.toUpperCase(Locale.ROOT))),
        state -> state.mode.name().toLowerCase(Locale.ROOT)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneTabletState> STREAM_CODEC = StreamCodec.of(
        (buffer, state) -> buffer.writeVarInt(state.mode.ordinal()),
        buffer -> new ArcaneTabletState(CURRENT_VERSION,
            Mode.values()[Math.floorMod(buffer.readVarInt(), Mode.values().length)])
    );

    public ArcaneTabletState {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported Arcane Tablet state");
        java.util.Objects.requireNonNull(mode, "mode");
    }

    public ArcaneTabletState next() {
        return new ArcaneTabletState(version, Mode.values()[(mode.ordinal() + 1) % Mode.values().length]);
    }

    public enum Mode { TRANSMUTATION, CRAFTING }
}
