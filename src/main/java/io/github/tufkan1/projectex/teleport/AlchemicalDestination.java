package io.github.tufkan1.projectex.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/** Immutable, bounded teleport target persisted by Alchemical Books. */
public record AlchemicalDestination(String name, String dimension, int x, int y, int z) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final Codec<AlchemicalDestination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("name").forGetter(AlchemicalDestination::name),
        Codec.STRING.fieldOf("dimension").forGetter(AlchemicalDestination::dimension),
        Codec.INT.fieldOf("x").forGetter(AlchemicalDestination::x),
        Codec.INT.fieldOf("y").forGetter(AlchemicalDestination::y),
        Codec.INT.fieldOf("z").forGetter(AlchemicalDestination::z)
    ).apply(instance, AlchemicalDestination::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemicalDestination> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeUtf(value.name, MAX_NAME_LENGTH);
            buffer.writeUtf(value.dimension, 256);
            buffer.writeInt(value.x); buffer.writeInt(value.y); buffer.writeInt(value.z);
        },
        buffer -> new AlchemicalDestination(buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(256),
            buffer.readInt(), buffer.readInt(), buffer.readInt())
    );

    public AlchemicalDestination {
        name = validateName(name);
        if (dimension == null || dimension.length() > 256) {
            throw new IllegalArgumentException("Invalid destination dimension");
        }
        Identifier.parse(dimension);
    }

    public static AlchemicalDestination at(String name, ServerPosition position) {
        return new AlchemicalDestination(name, position.dimension(), position.x(), position.y(), position.z());
    }

    public BlockPos pos() { return new BlockPos(x, y, z); }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
    }

    public static String validateName(String value) {
        if (value == null) throw new IllegalArgumentException("Destination name is missing");
        String stripped = value.strip();
        if (stripped.isEmpty() || stripped.length() > MAX_NAME_LENGTH || stripped.equalsIgnoreCase("@back")
            || stripped.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid destination name");
        }
        return stripped;
    }

    public record ServerPosition(String dimension, int x, int y, int z) { }
}
