package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Explicit client decision for a single-use server preview token. */
public record KnowledgeShareDecisionPayload(UUID token, boolean accepted) implements CustomPacketPayload {
    public static final Type<KnowledgeShareDecisionPayload> TYPE =
        new Type<>(ProjectEX.id("knowledge_share_decision_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeShareDecisionPayload> CODEC =
        StreamCodec.ofMember(KnowledgeShareDecisionPayload::write, KnowledgeShareDecisionPayload::read);

    @Override public Type<KnowledgeShareDecisionPayload> type() { return TYPE; }
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeUUID(token); buffer.writeBoolean(accepted); }
    private static KnowledgeShareDecisionPayload read(RegistryFriendlyByteBuf buffer) {
        return new KnowledgeShareDecisionPayload(buffer.readUUID(), buffer.readBoolean());
    }
}
