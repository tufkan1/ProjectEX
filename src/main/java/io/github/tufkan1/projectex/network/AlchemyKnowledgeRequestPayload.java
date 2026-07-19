package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bounded client request for one server-filtered learned-item page. */
public record AlchemyKnowledgeRequestPayload(
    int protocolVersion,
    long sessionId,
    long queryId,
    String query,
    int page,
    int pageSize
) implements CustomPacketPayload {
    public static final Type<AlchemyKnowledgeRequestPayload> TYPE =
        new Type<>(ProjectEX.id("alchemy_knowledge_request_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyKnowledgeRequestPayload> CODEC =
        StreamCodec.ofMember(AlchemyKnowledgeRequestPayload::write, AlchemyKnowledgeRequestPayload::read);

    public AlchemyKnowledgeRequestPayload {
        java.util.Objects.requireNonNull(query, "query");
        if (query.length() > AlchemyNetworkProtocol.MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("Search query exceeds protocol limit");
        }
    }

    public boolean hasValidShape() {
        return protocolVersion == AlchemyNetworkProtocol.VERSION
            && sessionId != 0
            && queryId >= 0
            && page >= 0
            && pageSize >= 1
            && pageSize <= AlchemyNetworkProtocol.MAX_KNOWLEDGE_PAGE_SIZE;
    }

    @Override
    public Type<AlchemyKnowledgeRequestPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(sessionId);
        buffer.writeLong(queryId);
        buffer.writeUtf(query, AlchemyNetworkProtocol.MAX_SEARCH_LENGTH);
        buffer.writeVarInt(page);
        buffer.writeVarInt(pageSize);
    }

    private static AlchemyKnowledgeRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new AlchemyKnowledgeRequestPayload(
            buffer.readVarInt(),
            buffer.readLong(),
            buffer.readLong(),
            buffer.readUtf(AlchemyNetworkProtocol.MAX_SEARCH_LENGTH),
            buffer.readVarInt(),
            buffer.readVarInt()
        );
    }
}
