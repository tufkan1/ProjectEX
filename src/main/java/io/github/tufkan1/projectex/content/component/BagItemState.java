package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.storage.BagIdentity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Persistent and synchronized form of a portable bag's identity. */
public record BagItemState(int version, UUID bagId, String color, Optional<UUID> owner) {
    public static final Codec<BagItemState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, BagIdentity.CURRENT_VERSION).fieldOf("version").forGetter(BagItemState::version),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("bag_id").forGetter(BagItemState::bagId),
            Codec.STRING.fieldOf("color").forGetter(BagItemState::color),
            Codec.STRING.optionalFieldOf("owner").xmap(
                value -> value.map(UUID::fromString), value -> value.map(UUID::toString)
            ).forGetter(BagItemState::owner)
        ).apply(instance, BagItemState::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BagItemState> STREAM_CODEC = StreamCodec.of(
        (buffer, state) -> {
            buffer.writeVarInt(state.version);
            buffer.writeUUID(state.bagId);
            buffer.writeUtf(state.color, 32);
            buffer.writeBoolean(state.owner.isPresent());
            state.owner.ifPresent(buffer::writeUUID);
        },
        buffer -> new BagItemState(
            buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(32),
            buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty()
        )
    );

    public BagItemState {
        new BagIdentity(version, bagId, color, owner);
    }

    public static BagItemState create(String color, UUID owner) {
        BagIdentity identity = BagIdentity.create(color, owner);
        return new BagItemState(identity.version(), identity.bagId(), identity.color(), identity.owner());
    }

    public boolean permits(UUID actor, boolean operator) {
        return new BagIdentity(version, bagId, color, owner).permits(actor, operator);
    }
}
