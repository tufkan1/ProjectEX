package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned charge for server-authoritative matter tool actions. */
public record MatterToolState(int version, int charge) {
    public static final int CURRENT_VERSION = 1;
    public static final MatterToolState DEFAULT = new MatterToolState(CURRENT_VERSION, 0);
    public static final Codec<MatterToolState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(MatterToolState::version),
            Codec.intRange(0, 16).fieldOf("charge").forGetter(MatterToolState::charge)
        ).apply(instance, MatterToolState::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MatterToolState> STREAM_CODEC = StreamCodec.of(
        (buffer, state) -> { buffer.writeVarInt(state.version); buffer.writeVarInt(state.charge); },
        buffer -> new MatterToolState(buffer.readVarInt(), buffer.readVarInt())
    );

    public MatterToolState {
        if (version != CURRENT_VERSION || charge < 0 || charge > 16) {
            throw new IllegalArgumentException("Invalid matter tool state");
        }
    }

    public MatterToolState next(int maxCharge) {
        if (maxCharge < 0 || maxCharge > 16) throw new IllegalArgumentException("Invalid max charge");
        return new MatterToolState(version, (charge + 1) % (maxCharge + 1));
    }
}
