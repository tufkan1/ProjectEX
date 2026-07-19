package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned owner binding and stack-owned destination set. */
public record AlchemicalBookState(int version, Optional<UUID> owner, AlchemicalBookLocations stackLocations) {
    public static final int CURRENT_VERSION = 1;
    public static final AlchemicalBookState EMPTY = new AlchemicalBookState(
        CURRENT_VERSION, Optional.empty(), AlchemicalBookLocations.EMPTY);
    public static final Codec<AlchemicalBookState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(AlchemicalBookState::version),
        Codec.STRING.optionalFieldOf("owner").xmap(value -> value.map(UUID::fromString),
            value -> value.map(UUID::toString)).forGetter(AlchemicalBookState::owner),
        AlchemicalBookLocations.CODEC.fieldOf("stack_locations").forGetter(AlchemicalBookState::stackLocations)
    ).apply(instance, AlchemicalBookState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemicalBookState> STREAM_CODEC = StreamCodec.of(
        AlchemicalBookState::write, AlchemicalBookState::read);

    public AlchemicalBookState {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported Alchemical Book state");
        java.util.Objects.requireNonNull(owner, "owner");
        java.util.Objects.requireNonNull(stackLocations, "stackLocations");
    }

    public AlchemicalBookState bind(UUID playerId) {
        return new AlchemicalBookState(version, Optional.of(playerId), stackLocations);
    }

    public AlchemicalBookState unbind() {
        return new AlchemicalBookState(version, Optional.empty(), stackLocations);
    }

    public AlchemicalBookState withStackLocations(AlchemicalBookLocations replacement) {
        return new AlchemicalBookState(version, owner, replacement);
    }

    private static void write(RegistryFriendlyByteBuf buffer, AlchemicalBookState state) {
        buffer.writeVarInt(state.version);
        buffer.writeBoolean(state.owner.isPresent());
        state.owner.ifPresent(buffer::writeUUID);
        buffer.writeVarInt(state.stackLocations.destinations().size());
        state.stackLocations.destinations().forEach(value -> AlchemicalDestination.STREAM_CODEC.encode(buffer, value));
        buffer.writeBoolean(state.stackLocations.back().isPresent());
        state.stackLocations.back().ifPresent(value -> AlchemicalDestination.STREAM_CODEC.encode(buffer, value));
    }

    private static AlchemicalBookState read(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readVarInt();
        Optional<UUID> owner = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        int size = buffer.readVarInt();
        if (size < 0 || size > AlchemicalBookLocations.MAX_DESTINATIONS) {
            throw new IllegalArgumentException("Oversized Alchemical Book payload");
        }
        ArrayList<AlchemicalDestination> destinations = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            destinations.add(AlchemicalDestination.STREAM_CODEC.decode(buffer));
        }
        Optional<AlchemicalDestination> back = buffer.readBoolean()
            ? Optional.of(AlchemicalDestination.STREAM_CODEC.decode(buffer)) : Optional.empty();
        return new AlchemicalBookState(version, owner, new AlchemicalBookLocations(destinations, back));
    }
}
