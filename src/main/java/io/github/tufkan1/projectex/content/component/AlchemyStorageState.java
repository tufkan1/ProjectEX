package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.machine.MachineAccess;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned exact EMC buffer and ownership carried through saves and block items. */
public record AlchemyStorageState(int version, EmcValue stored, MachineAccess access) {
    public static final int CURRENT_VERSION = 1;
    private static final int MAX_DIGITS = 4096;
    private static final Codec<EmcValue> EMC_CODEC = Codec.STRING.comapFlatMap(
        value -> {
            try {
                if (value.isEmpty() || value.length() > MAX_DIGITS) throw new IllegalArgumentException();
                return DataResult.success(new EmcValue(new BigInteger(value)));
            } catch (IllegalArgumentException exception) {
                return DataResult.error(() -> "Invalid alchemy storage EMC");
            }
        }, value -> value.amount().toString()
    );
    public static final Codec<AlchemyStorageState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(AlchemyStorageState::version),
            EMC_CODEC.fieldOf("stored").forGetter(AlchemyStorageState::stored),
            Codec.STRING.optionalFieldOf("owner").xmap(
                value -> value.map(UUID::fromString),
                value -> value.map(UUID::toString)
            ).forGetter(state -> state.access.owner()),
            Codec.BOOL.optionalFieldOf("public_access", false)
                .forGetter(state -> state.access.publicAccess())
        ).apply(instance, (version, stored, owner, publicAccess) ->
            new AlchemyStorageState(version, stored, new MachineAccess(owner, publicAccess)))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyStorageState> STREAM_CODEC =
        StreamCodec.of(
            (buffer, state) -> {
                buffer.writeVarInt(state.version);
                buffer.writeUtf(state.stored.amount().toString(), MAX_DIGITS);
                buffer.writeBoolean(state.access.owner().isPresent());
                state.access.owner().ifPresent(buffer::writeUUID);
                buffer.writeBoolean(state.access.publicAccess());
            },
            buffer -> new AlchemyStorageState(
                buffer.readVarInt(),
                new EmcValue(new BigInteger(buffer.readUtf(MAX_DIGITS))),
                new MachineAccess(
                    buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty(),
                    buffer.readBoolean()
                )
            )
        );

    public AlchemyStorageState {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported storage state");
        java.util.Objects.requireNonNull(stored, "stored");
        java.util.Objects.requireNonNull(access, "access");
    }

    public static AlchemyStorageState empty() {
        return new AlchemyStorageState(CURRENT_VERSION, EmcValue.ZERO, MachineAccess.UNCLAIMED);
    }
}
