package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned arbitrary-precision amount persisted directly on an item stack. */
public record PortableEmcState(int version, EmcValue stored) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_DIGITS = 4096;
    public static final PortableEmcState EMPTY = new PortableEmcState(
        CURRENT_VERSION,
        EmcValue.ZERO
    );
    private static final Codec<EmcValue> VALUE_CODEC = Codec.STRING.comapFlatMap(
        PortableEmcState::parse,
        value -> value.amount().toString()
    );
    private static final Codec<PortableEmcState> CURRENT_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, CURRENT_VERSION).fieldOf("version")
                .forGetter(PortableEmcState::version),
            VALUE_CODEC.fieldOf("stored").forGetter(PortableEmcState::stored)
        ).apply(instance, PortableEmcState::new)
    );
    public static final Codec<PortableEmcState> CODEC = CURRENT_CODEC.withAlternative(
        VALUE_CODEC.fieldOf("emc").codec(),
        value -> new PortableEmcState(CURRENT_VERSION, value)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableEmcState> STREAM_CODEC =
        StreamCodec.of(
            (buffer, state) -> {
                buffer.writeVarInt(state.version);
                buffer.writeUtf(state.stored.amount().toString(), MAX_DIGITS);
            },
            buffer -> new PortableEmcState(
                buffer.readVarInt(),
                parseOrThrow(buffer.readUtf(MAX_DIGITS))
            )
        );

    public PortableEmcState {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported portable EMC version: " + version);
        }
        java.util.Objects.requireNonNull(stored, "stored");
        if (stored.amount().toString().length() > MAX_DIGITS) {
            throw new IllegalArgumentException("Portable EMC exceeds safe digit limit");
        }
    }

    private static DataResult<EmcValue> parse(String value) {
        try {
            return DataResult.success(parseOrThrow(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static EmcValue parseOrThrow(String value) {
        if (value.isEmpty() || value.length() > MAX_DIGITS) {
            throw new IllegalArgumentException("Invalid portable EMC length");
        }
        try {
            return new EmcValue(new BigInteger(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid portable EMC amount", exception);
        }
    }
}
