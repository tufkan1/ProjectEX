package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionResult;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.math.BigInteger;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Minimal authoritative response; it never mirrors client-provided EMC amounts. */
public record AlchemyResultPayload(
    int protocolVersion,
    long sessionId,
    long requestId,
    boolean success,
    int failureId,
    long emcRevision,
    String balance,
    int knowledgeCount
) implements CustomPacketPayload {
    public static final Type<AlchemyResultPayload> TYPE = new Type<>(ProjectEX.id("alchemy_result_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyResultPayload> CODEC = StreamCodec.ofMember(
        AlchemyResultPayload::write,
        AlchemyResultPayload::read
    );

    public AlchemyResultPayload {
        java.util.Objects.requireNonNull(balance, "balance");
        if (balance.length() > AlchemyNetworkProtocol.MAX_BALANCE_LENGTH) {
            throw new IllegalArgumentException("Balance exceeds protocol limit");
        }
        if (knowledgeCount < 0) {
            throw new IllegalArgumentException("Knowledge count cannot be negative");
        }
    }

    public static AlchemyResultPayload from(
        long sessionId,
        long requestId,
        long revision,
        AlchemyTransactionResult result
    ) {
        return new AlchemyResultPayload(
            AlchemyNetworkProtocol.VERSION,
            sessionId,
            requestId,
            result.success(),
            result.failure().ordinal(),
            revision,
            result.player().balance().amount().toString(),
            result.player().knowledge().size()
        );
    }

    public static AlchemyResultPayload rejected(
        long sessionId,
        long requestId,
        long revision,
        AlchemyTransactionFailure failure,
        PlayerAlchemyState player
    ) {
        return new AlchemyResultPayload(
            AlchemyNetworkProtocol.VERSION,
            sessionId,
            requestId,
            false,
            failure.ordinal(),
            revision,
            player.balance().amount().toString(),
            player.knowledge().size()
        );
    }

    public Optional<AlchemyTransactionFailure> failure() {
        AlchemyTransactionFailure[] values = AlchemyTransactionFailure.values();
        return failureId >= 0 && failureId < values.length ? Optional.of(values[failureId]) : Optional.empty();
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
        Optional<AlchemyTransactionFailure> decodedFailure = failure();
        return protocolVersion == AlchemyNetworkProtocol.VERSION
            && decodedFailure.isPresent()
            && success == (decodedFailure.orElseThrow() == AlchemyTransactionFailure.NONE)
            && parsedBalance().isPresent();
    }

    @Override
    public Type<AlchemyResultPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(sessionId);
        buffer.writeLong(requestId);
        buffer.writeBoolean(success);
        buffer.writeVarInt(failureId);
        buffer.writeLong(emcRevision);
        buffer.writeUtf(balance, AlchemyNetworkProtocol.MAX_BALANCE_LENGTH);
        buffer.writeVarInt(knowledgeCount);
    }

    private static AlchemyResultPayload read(RegistryFriendlyByteBuf buffer) {
        return new AlchemyResultPayload(
            buffer.readVarInt(),
            buffer.readLong(),
            buffer.readLong(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readLong(),
            buffer.readUtf(AlchemyNetworkProtocol.MAX_BALANCE_LENGTH),
            buffer.readVarInt()
        );
    }
}
