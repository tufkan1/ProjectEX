package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Complete bounded server view used to open or refresh the accessible book screen. */
public record AlchemicalBookViewPayload(
    UUID sessionId, long requestId, int tier, boolean editable, String balance, String failure,
    List<Entry> entries, Optional<Entry> back
) implements CustomPacketPayload {
    public static final int MAX_COST_LENGTH = 128;
    public static final Type<AlchemicalBookViewPayload> TYPE =
        new Type<>(ProjectEX.id("alchemical_book_view_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemicalBookViewPayload> CODEC =
        StreamCodec.ofMember(AlchemicalBookViewPayload::write, AlchemicalBookViewPayload::read);

    public AlchemicalBookViewPayload {
        entries = List.copyOf(entries);
        if (tier < 0 || tier > 3 || balance.length() > 4_096 || failure.length() > 64
            || entries.size() > AlchemicalBookLocations.MAX_DESTINATIONS) {
            throw new IllegalArgumentException("Malformed Alchemical Book view");
        }
    }

    @Override public Type<AlchemicalBookViewPayload> type() { return TYPE; }
    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId); buffer.writeVarLong(requestId); buffer.writeVarInt(tier);
        buffer.writeBoolean(editable); buffer.writeUtf(balance, 4_096); buffer.writeUtf(failure, 64);
        buffer.writeVarInt(entries.size()); entries.forEach(entry -> entry.write(buffer));
        buffer.writeBoolean(back.isPresent()); back.ifPresent(entry -> entry.write(buffer));
    }
    private static AlchemicalBookViewPayload read(RegistryFriendlyByteBuf buffer) {
        UUID session = buffer.readUUID(); long request = buffer.readVarLong(); int tier = buffer.readVarInt();
        boolean editable = buffer.readBoolean(); String balance = buffer.readUtf(4_096);
        String failure = buffer.readUtf(64); int size = buffer.readVarInt();
        if (size < 0 || size > AlchemicalBookLocations.MAX_DESTINATIONS) {
            throw new IllegalArgumentException("Oversized Alchemical Book view");
        }
        ArrayList<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) entries.add(Entry.read(buffer));
        Optional<Entry> back = buffer.readBoolean() ? Optional.of(Entry.read(buffer)) : Optional.empty();
        return new AlchemicalBookViewPayload(session, request, tier, editable, balance, failure, entries, back);
    }

    public record Entry(AlchemicalDestination destination, String cost) {
        public Entry { if (cost.length() > MAX_COST_LENGTH) throw new IllegalArgumentException("Oversized teleport price"); }
        private void write(RegistryFriendlyByteBuf buffer) {
            AlchemicalDestination.STREAM_CODEC.encode(buffer, destination); buffer.writeUtf(cost, MAX_COST_LENGTH);
        }
        private static Entry read(RegistryFriendlyByteBuf buffer) {
            return new Entry(AlchemicalDestination.STREAM_CODEC.decode(buffer), buffer.readUtf(MAX_COST_LENGTH));
        }
    }
}
