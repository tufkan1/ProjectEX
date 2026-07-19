package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bounded server-authoritative componentless EMC values used only for item tooltips. */
public record EmcTooltipSyncPayload(int protocolVersion, long revision, List<Entry> entries)
    implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 65_536;
    public static final Type<EmcTooltipSyncPayload> TYPE =
        new Type<>(ProjectEX.id("emc_tooltips_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmcTooltipSyncPayload> CODEC =
        StreamCodec.ofMember(EmcTooltipSyncPayload::write, EmcTooltipSyncPayload::read);

    public EmcTooltipSyncPayload {
        entries = List.copyOf(entries);
        if (revision < 0 || entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid EMC tooltip snapshot");
        }
    }

    public boolean isStructurallyValid() {
        return protocolVersion == AlchemyNetworkProtocol.VERSION
            && revision >= 0 && entries.stream().allMatch(Entry::isStructurallyValid);
    }

    @Override public Type<EmcTooltipSyncPayload> type() { return TYPE; }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(protocolVersion);
        buffer.writeLong(revision);
        buffer.writeVarInt(entries.size());
        entries.forEach(entry -> entry.write(buffer));
    }

    private static EmcTooltipSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int protocolVersion = buffer.readVarInt();
        long revision = buffer.readLong();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new io.netty.handler.codec.DecoderException("Invalid EMC tooltip entry count: " + size);
        }
        ArrayList<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) entries.add(Entry.read(buffer));
        return new EmcTooltipSyncPayload(protocolVersion, revision, entries);
    }

    public record Entry(String itemId, String emc) {
        public Entry {
            java.util.Objects.requireNonNull(itemId, "itemId");
            java.util.Objects.requireNonNull(emc, "emc");
            if (itemId.length() > AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH
                || emc.length() > AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH) {
                throw new IllegalArgumentException("EMC tooltip entry exceeds protocol limit");
            }
        }
        public boolean isStructurallyValid() {
            try {
                return EmcKey.parse(itemId) != null && new BigInteger(emc).signum() > 0;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(itemId, AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH);
            buffer.writeUtf(emc, AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH);
        }
        private static Entry read(RegistryFriendlyByteBuf buffer) {
            return new Entry(buffer.readUtf(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH),
                buffer.readUtf(AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH));
        }
    }
}
