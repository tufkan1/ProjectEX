package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Replay-resistant action inside a server-created Alchemical Book session. */
public record AlchemicalBookActionPayload(
    int protocolVersion, UUID sessionId, long requestId, int action, String name
) implements CustomPacketPayload {
    public static final int PROTOCOL_VERSION = 1;
    public static final Type<AlchemicalBookActionPayload> TYPE =
        new Type<>(ProjectEX.id("alchemical_book_action_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemicalBookActionPayload> CODEC =
        StreamCodec.ofMember(AlchemicalBookActionPayload::write, AlchemicalBookActionPayload::read);

    public AlchemicalBookActionPayload {
        if (name.length() > AlchemicalDestination.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Alchemical Book name is oversized");
        }
    }

    @Override public Type<AlchemicalBookActionPayload> type() { return TYPE; }
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion); buffer.writeUUID(sessionId); buffer.writeVarLong(requestId);
        buffer.writeVarInt(action); buffer.writeUtf(name, AlchemicalDestination.MAX_NAME_LENGTH);
    }
    private static AlchemicalBookActionPayload read(RegistryFriendlyByteBuf buffer) {
        return new AlchemicalBookActionPayload(buffer.readVarInt(), buffer.readUUID(), buffer.readVarLong(),
            buffer.readVarInt(), buffer.readUtf(AlchemicalDestination.MAX_NAME_LENGTH));
    }
}
