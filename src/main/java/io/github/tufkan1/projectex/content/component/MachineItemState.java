package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.machine.MachineAccess;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.machine.MachineState;
import io.github.tufkan1.projectex.machine.MachineTier;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Exact machine state carried by a broken block item. */
public record MachineItemState(
    int version,
    MachineTier tier,
    EmcValue stored,
    BigInteger deferredGeneration,
    Optional<UUID> owner,
    boolean publicAccess,
    MachineRedstoneMode redstoneMode
) {
    private static final int MAX_DIGITS = 4096;
    private static final Codec<BigInteger> DECIMAL = Codec.STRING.comapFlatMap(
        MachineItemState::parseDecimal,
        BigInteger::toString
    );
    private static final Codec<MachineTier> TIER = Codec.STRING.xmap(MachineTier::valueOf, MachineTier::name);
    private static final Codec<MachineRedstoneMode> REDSTONE = Codec.STRING.xmap(
        MachineRedstoneMode::valueOf,
        MachineRedstoneMode::name
    );
    private static final Codec<MachineItemState> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, MachineState.CURRENT_VERSION).fieldOf("version").forGetter(MachineItemState::version),
        TIER.fieldOf("tier").forGetter(MachineItemState::tier),
        DECIMAL.fieldOf("stored").xmap(EmcValue::new, EmcValue::amount).forGetter(MachineItemState::stored),
        DECIMAL.optionalFieldOf("deferred_generation", BigInteger.ZERO).forGetter(MachineItemState::deferredGeneration),
        Codec.STRING.optionalFieldOf("owner").xmap(
            value -> value.map(UUID::fromString),
            value -> value.map(UUID::toString)
        ).forGetter(MachineItemState::owner),
        Codec.BOOL.optionalFieldOf("public_access", false).forGetter(MachineItemState::publicAccess),
        REDSTONE.optionalFieldOf("redstone_mode", MachineRedstoneMode.IGNORED).forGetter(MachineItemState::redstoneMode)
    ).apply(instance, MachineItemState::new));
    public static final Codec<MachineItemState> CODEC = RAW_CODEC.validate(MachineItemState::validate);
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineItemState> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeVarInt(value.version);
            buffer.writeUtf(value.tier.name());
            buffer.writeUtf(value.stored.amount().toString(), MAX_DIGITS);
            buffer.writeUtf(value.deferredGeneration.toString(), MAX_DIGITS);
            buffer.writeBoolean(value.owner.isPresent());
            value.owner.ifPresent(buffer::writeUUID);
            buffer.writeBoolean(value.publicAccess);
            buffer.writeUtf(value.redstoneMode.name());
        },
        buffer -> validateOrThrow(new MachineItemState(
            buffer.readVarInt(),
            MachineTier.valueOf(buffer.readUtf(64)),
            new EmcValue(parseDecimalOrThrow(buffer.readUtf(MAX_DIGITS))),
            parseDecimalOrThrow(buffer.readUtf(MAX_DIGITS)),
            buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty(),
            buffer.readBoolean(),
            MachineRedstoneMode.valueOf(buffer.readUtf(64))
        ))
    );

    public MachineItemState {
        java.util.Objects.requireNonNull(tier, "tier");
        java.util.Objects.requireNonNull(stored, "stored");
        java.util.Objects.requireNonNull(deferredGeneration, "deferredGeneration");
        java.util.Objects.requireNonNull(owner, "owner");
        java.util.Objects.requireNonNull(redstoneMode, "redstoneMode");
    }

    public static MachineItemState from(MachineState state) {
        return new MachineItemState(
            state.version(), state.tier(), state.stored(), state.deferredGeneration(),
            state.access().owner(), state.access().publicAccess(), state.redstoneMode()
        );
    }

    public MachineState toMachineState() {
        return new MachineState(
            version, tier, stored, deferredGeneration,
            new MachineAccess(owner, publicAccess), redstoneMode
        );
    }

    private static DataResult<BigInteger> parseDecimal(String value) {
        try {
            return DataResult.success(parseDecimalOrThrow(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DataResult<MachineItemState> validate(MachineItemState value) {
        try {
            value.toMachineState();
            return DataResult.success(value);
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static MachineItemState validateOrThrow(MachineItemState value) {
        value.toMachineState();
        return value;
    }

    private static BigInteger parseDecimalOrThrow(String value) {
        if (value.isEmpty() || value.length() > MAX_DIGITS || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid machine state decimal");
        }
        return new BigInteger(value);
    }
}
