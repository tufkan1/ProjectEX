package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-authored summary shown before a knowledge mutation can be confirmed. */
public record KnowledgeSharePreviewPayload(
    UUID token, UUID ownerId, int mode, int added, int removed, int duplicates, int resultSize, long expiresAt
) implements CustomPacketPayload {
    public static final Type<KnowledgeSharePreviewPayload> TYPE =
        new Type<>(ProjectEX.id("knowledge_share_preview_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeSharePreviewPayload> CODEC =
        StreamCodec.ofMember(KnowledgeSharePreviewPayload::write, KnowledgeSharePreviewPayload::read);

    public KnowledgeSharePreviewPayload {
        if (mode < 0 || mode > 1 || added < 0 || removed < 0 || duplicates < 0 || resultSize < 0) {
            throw new IllegalArgumentException("Malformed knowledge preview");
        }
    }

    @Override public Type<KnowledgeSharePreviewPayload> type() { return TYPE; }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(token);
        buffer.writeUUID(ownerId);
        buffer.writeVarInt(mode);
        buffer.writeVarInt(added);
        buffer.writeVarInt(removed);
        buffer.writeVarInt(duplicates);
        buffer.writeVarInt(resultSize);
        buffer.writeLong(expiresAt);
    }

    private static KnowledgeSharePreviewPayload read(RegistryFriendlyByteBuf buffer) {
        return new KnowledgeSharePreviewPayload(buffer.readUUID(), buffer.readUUID(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readLong());
    }
}
