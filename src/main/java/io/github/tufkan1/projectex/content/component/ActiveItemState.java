package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned, persistent charge and mode shared by server-authoritative active items. */
public record ActiveItemState(int version, int charge, ActiveItemMode mode) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_CHARGE = 2;
    public static final ActiveItemState DEFAULT = new ActiveItemState(
        CURRENT_VERSION,
        0,
        ActiveItemMode.CUBE
    );
    public static final Codec<ActiveItemState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(ActiveItemState::version),
            Codec.intRange(0, MAX_CHARGE).fieldOf("charge").forGetter(ActiveItemState::charge),
            Codec.STRING.xmap(ActiveItemMode::parse, ActiveItemMode::serializedName)
                .fieldOf("mode").forGetter(ActiveItemState::mode)
        ).apply(instance, ActiveItemState::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveItemState> STREAM_CODEC =
        StreamCodec.of(
            (buffer, state) -> {
                buffer.writeVarInt(state.version);
                buffer.writeVarInt(state.charge);
                buffer.writeVarInt(state.mode.ordinal());
            },
            buffer -> new ActiveItemState(
                buffer.readVarInt(),
                buffer.readVarInt(),
                ActiveItemMode.values()[Math.floorMod(buffer.readVarInt(), ActiveItemMode.values().length)]
            )
        );

    public ActiveItemState {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported active item state version: " + version);
        }
        if (charge < 0 || charge > MAX_CHARGE) {
            throw new IllegalArgumentException("Charge outside supported range: " + charge);
        }
        java.util.Objects.requireNonNull(mode, "mode");
    }

    public ActiveItemState nextCharge() {
        return new ActiveItemState(version, (charge + 1) % (MAX_CHARGE + 1), mode);
    }

    public ActiveItemState nextMode() {
        return new ActiveItemState(version, charge, mode.next());
    }
}
