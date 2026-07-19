package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** One bounded authoritative knowledge/search page. */
public record AlchemyKnowledgePagePayload(
    int protocolVersion,
    long sessionId,
    long queryId,
    int failureId,
    int page,
    int totalPages,
    int totalEntries,
    List<Entry> entries
) implements CustomPacketPayload {
    public static final Type<AlchemyKnowledgePagePayload> TYPE =
        new Type<>(ProjectEX.id("alchemy_knowledge_page_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyKnowledgePagePayload> CODEC =
        StreamCodec.ofMember(AlchemyKnowledgePagePayload::write, AlchemyKnowledgePagePayload::read);

    public AlchemyKnowledgePagePayload {
        entries = List.copyOf(entries);
        if (entries.size() > AlchemyNetworkProtocol.MAX_KNOWLEDGE_PAGE_SIZE) {
            throw new IllegalArgumentException("Knowledge page exceeds protocol limit");
        }
    }

    public Optional<AlchemyTransactionFailure> failure() {
        AlchemyTransactionFailure[] failures = AlchemyTransactionFailure.values();
        return failureId >= 0 && failureId < failures.length ? Optional.of(failures[failureId]) : Optional.empty();
    }

    public boolean isStructurallyValid() {
        Optional<AlchemyTransactionFailure> decodedFailure = failure();
        if (protocolVersion != AlchemyNetworkProtocol.VERSION) {
            return false;
        }
        if (sessionId == 0 || queryId < 0 || decodedFailure.isEmpty()
            || page < 0 || totalPages < 0 || totalEntries < 0
            || !entries.stream().allMatch(Entry::isStructurallyValid)) {
            return false;
        }
        if (decodedFailure.orElseThrow() != AlchemyTransactionFailure.NONE) {
            return entries.isEmpty() && page == 0 && totalPages == 0 && totalEntries == 0;
        }
        if (totalEntries == 0) {
            return totalPages == 0 && page == 0 && entries.isEmpty();
        }
        return totalPages > 0 && page < totalPages && !entries.isEmpty() && entries.size() <= totalEntries;
    }

    @Override
    public Type<AlchemyKnowledgePagePayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(sessionId);
        buffer.writeLong(queryId);
        buffer.writeVarInt(failureId);
        buffer.writeVarInt(page);
        buffer.writeVarInt(totalPages);
        buffer.writeVarInt(totalEntries);
        buffer.writeVarInt(entries.size());
        entries.forEach(entry -> entry.write(buffer));
    }

    private static AlchemyKnowledgePagePayload read(RegistryFriendlyByteBuf buffer) {
        int protocolVersion = buffer.readVarInt();
        long sessionId = buffer.readLong();
        long queryId = buffer.readLong();
        int failureId = buffer.readVarInt();
        int page = buffer.readVarInt();
        int totalPages = buffer.readVarInt();
        int totalEntries = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > AlchemyNetworkProtocol.MAX_KNOWLEDGE_PAGE_SIZE) {
            throw new io.netty.handler.codec.DecoderException("Invalid knowledge page size: " + size);
        }
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(Entry.read(buffer));
        }
        return new AlchemyKnowledgePagePayload(
            protocolVersion, sessionId, queryId, failureId, page, totalPages, totalEntries, entries);
    }

    public record Entry(String itemId, String emc) {
        public Entry {
            java.util.Objects.requireNonNull(itemId, "itemId");
            java.util.Objects.requireNonNull(emc, "emc");
            if (itemId.length() > AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH
                || emc.length() > AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH) {
                throw new IllegalArgumentException("Knowledge entry exceeds protocol limit");
            }
        }

        public Optional<BigInteger> parsedEmc() {
            try {
                BigInteger parsed = new BigInteger(emc);
                return parsed.signum() > 0 ? Optional.of(parsed) : Optional.empty();
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        public boolean isStructurallyValid() {
            try {
                io.github.tufkan1.projectex.api.emc.EmcKey.parse(itemId);
                return parsedEmc().isPresent();
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(itemId, AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH);
            buffer.writeUtf(emc, AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH);
        }

        private static Entry read(RegistryFriendlyByteBuf buffer) {
            return new Entry(
                buffer.readUtf(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH),
                buffer.readUtf(AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH)
            );
        }
    }
}
