package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bounded server result for preview/confirmation feedback. */
public record KnowledgeShareResultPayload(boolean success, String reason, int learned, int total)
    implements CustomPacketPayload {
    public static final Type<KnowledgeShareResultPayload> TYPE =
        new Type<>(ProjectEX.id("knowledge_share_result_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeShareResultPayload> CODEC =
        StreamCodec.ofMember(KnowledgeShareResultPayload::write, KnowledgeShareResultPayload::read);

    public KnowledgeShareResultPayload {
        if (reason.length() > 64 || learned < 0 || total < 0) throw new IllegalArgumentException("Malformed result");
    }

    @Override public Type<KnowledgeShareResultPayload> type() { return TYPE; }
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(success); buffer.writeUtf(reason, 64); buffer.writeVarInt(learned); buffer.writeVarInt(total);
    }
    private static KnowledgeShareResultPayload read(RegistryFriendlyByteBuf buffer) {
        return new KnowledgeShareResultPayload(buffer.readBoolean(), buffer.readUtf(64),
            buffer.readVarInt(), buffer.readVarInt());
    }
}
