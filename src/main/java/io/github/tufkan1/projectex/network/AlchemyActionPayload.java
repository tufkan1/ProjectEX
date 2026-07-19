package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.alchemy.AlchemyTransaction;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Raw bounded client intent. Conversion to a trusted domain transaction is explicit. */
public record AlchemyActionPayload(
    int protocolVersion,
    long sessionId,
    long requestId,
    int operationId,
    String itemId,
    int count,
    long emcRevision
) implements CustomPacketPayload {
    public static final Type<AlchemyActionPayload> TYPE = new Type<>(ProjectEX.id("alchemy_action_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyActionPayload> CODEC = StreamCodec.ofMember(
        AlchemyActionPayload::write,
        AlchemyActionPayload::read
    );

    public AlchemyActionPayload {
        java.util.Objects.requireNonNull(itemId, "itemId");
        if (itemId.length() > AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH) {
            throw new IllegalArgumentException("Item identifier exceeds protocol limit");
        }
    }

    @Override
    public Type<AlchemyActionPayload> type() {
        return TYPE;
    }

    public Optional<AlchemyTransaction> toTransaction() {
        EmcMatch item;
        try {
            item = EmcMatch.item(EmcKey.parse(itemId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return switch (operationId) {
            case 0 -> count == 1
                ? Optional.of(new AlchemyTransaction.Learn(item, emcRevision))
                : Optional.empty();
            case 1 -> Optional.of(new AlchemyTransaction.Burn(item, count, emcRevision));
            case 2 -> Optional.of(new AlchemyTransaction.Create(item, count, emcRevision));
            default -> Optional.empty();
        };
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(sessionId);
        buffer.writeLong(requestId);
        buffer.writeVarInt(operationId);
        buffer.writeUtf(itemId, AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH);
        buffer.writeVarInt(count);
        buffer.writeLong(emcRevision);
    }

    private static AlchemyActionPayload read(RegistryFriendlyByteBuf buffer) {
        return new AlchemyActionPayload(
            buffer.readVarInt(),
            buffer.readLong(),
            buffer.readLong(),
            buffer.readVarInt(),
            buffer.readUtf(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH),
            buffer.readVarInt(),
            buffer.readLong()
        );
    }
}
