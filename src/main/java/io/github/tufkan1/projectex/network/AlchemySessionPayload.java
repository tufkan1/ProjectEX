package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.math.BigInteger;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-created initial state for one transmutation menu session. */
public record AlchemySessionPayload(
    int protocolVersion,
    long sessionId,
    long emcRevision,
    String balance,
    int knowledgeCount
) implements CustomPacketPayload {
    public static final Type<AlchemySessionPayload> TYPE = new Type<>(ProjectEX.id("alchemy_session_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemySessionPayload> CODEC = StreamCodec.ofMember(
        AlchemySessionPayload::write,
        AlchemySessionPayload::read
    );

    public AlchemySessionPayload {
        java.util.Objects.requireNonNull(balance, "balance");
        if (balance.length() > AlchemyNetworkProtocol.MAX_BALANCE_LENGTH) {
            throw new IllegalArgumentException("Balance exceeds protocol limit");
        }
        if (knowledgeCount < 0) {
            throw new IllegalArgumentException("Knowledge count cannot be negative");
        }
    }

    public static AlchemySessionPayload create(
        long sessionId,
        long emcRevision,
        PlayerAlchemyState player
    ) {
        return new AlchemySessionPayload(
            AlchemyNetworkProtocol.VERSION,
            sessionId,
            emcRevision,
            player.balance().amount().toString(),
            player.knowledge().size()
        );
    }

    public Optional<BigInteger> parsedBalance() {
        try {
            BigInteger parsed = new BigInteger(balance);
            return parsed.signum() < 0 ? Optional.empty() : Optional.of(parsed);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public boolean isStructurallyValid() {
        return protocolVersion == AlchemyNetworkProtocol.VERSION
            && sessionId != 0
            && parsedBalance().isPresent();
    }

    @Override
    public Type<AlchemySessionPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(sessionId);
        buffer.writeLong(emcRevision);
        buffer.writeUtf(balance, AlchemyNetworkProtocol.MAX_BALANCE_LENGTH);
        buffer.writeVarInt(knowledgeCount);
    }

    private static AlchemySessionPayload read(RegistryFriendlyByteBuf buffer) {
        return new AlchemySessionPayload(
            buffer.readVarInt(),
            buffer.readLong(),
            buffer.readLong(),
            buffer.readUtf(AlchemyNetworkProtocol.MAX_BALANCE_LENGTH),
            buffer.readVarInt()
        );
    }
}
