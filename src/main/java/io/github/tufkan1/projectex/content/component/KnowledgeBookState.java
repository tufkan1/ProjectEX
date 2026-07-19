package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.knowledge.KnowledgeShareWorkflow;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshot;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned, signed knowledge payload carried by one sharing book stack. */
public record KnowledgeBookState(int version, KnowledgeSnapshot snapshot, KnowledgeShareWorkflow.Mode mode) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<KnowledgeBookState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(KnowledgeBookState::version),
        Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("snapshot_id")
            .forGetter(state -> state.snapshot.snapshotId()),
        Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("owner_id")
            .forGetter(state -> state.snapshot.ownerId()),
        Codec.LONG.fieldOf("issued_at").forGetter(state -> state.snapshot.issuedAt()),
        Codec.LONG.fieldOf("expires_at").forGetter(state -> state.snapshot.expiresAt()),
        Codec.STRING.listOf(0, KnowledgeSnapshot.MAX_ENTRIES).fieldOf("knowledge")
            .forGetter(state -> state.snapshot.knowledge().stream().map(EmcKey::toString).toList()),
        Codec.STRING.fieldOf("signature").forGetter(state ->
            Base64.getEncoder().encodeToString(state.snapshot.signature())),
        Codec.STRING.xmap(value -> KnowledgeShareWorkflow.Mode.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)).fieldOf("mode").forGetter(KnowledgeBookState::mode)
    ).apply(instance, KnowledgeBookState::decode));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeBookState> STREAM_CODEC = StreamCodec.of(
        KnowledgeBookState::write, KnowledgeBookState::read);

    public KnowledgeBookState {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported Knowledge Book state");
        java.util.Objects.requireNonNull(snapshot, "snapshot");
        java.util.Objects.requireNonNull(mode, "mode");
    }

    public KnowledgeBookState withSnapshot(KnowledgeSnapshot replacement) {
        return new KnowledgeBookState(version, replacement, mode);
    }

    public KnowledgeBookState nextMode() {
        return new KnowledgeBookState(version, snapshot,
            mode == KnowledgeShareWorkflow.Mode.MERGE
                ? KnowledgeShareWorkflow.Mode.REPLACE : KnowledgeShareWorkflow.Mode.MERGE);
    }

    private static KnowledgeBookState decode(
        int version, UUID snapshotId, UUID ownerId, long issuedAt, long expiresAt,
        List<String> knowledge, String signature, KnowledgeShareWorkflow.Mode mode
    ) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Malformed Knowledge Book signature", exception);
        }
        return new KnowledgeBookState(version, new KnowledgeSnapshot(
            KnowledgeSnapshot.CURRENT_VERSION, snapshotId, ownerId, issuedAt, expiresAt,
            knowledge.stream().map(EmcKey::parse).toList(), bytes), mode);
    }

    private static void write(RegistryFriendlyByteBuf buffer, KnowledgeBookState state) {
        buffer.writeVarInt(state.version);
        buffer.writeUUID(state.snapshot.snapshotId());
        buffer.writeUUID(state.snapshot.ownerId());
        buffer.writeLong(state.snapshot.issuedAt());
        buffer.writeLong(state.snapshot.expiresAt());
        buffer.writeVarInt(state.snapshot.knowledge().size());
        state.snapshot.knowledge().forEach(key -> buffer.writeUtf(key.toString(), 256));
        buffer.writeByteArray(state.snapshot.signature());
        buffer.writeVarInt(state.mode.ordinal());
    }

    private static KnowledgeBookState read(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readVarInt();
        UUID snapshotId = buffer.readUUID();
        UUID ownerId = buffer.readUUID();
        long issuedAt = buffer.readLong();
        long expiresAt = buffer.readLong();
        int size = buffer.readVarInt();
        if (size < 0 || size > KnowledgeSnapshot.MAX_ENTRIES) {
            throw new IllegalArgumentException("Oversized Knowledge Book payload");
        }
        java.util.ArrayList<EmcKey> knowledge = new java.util.ArrayList<>(size);
        for (int index = 0; index < size; index++) knowledge.add(EmcKey.parse(buffer.readUtf(256)));
        byte[] signature = buffer.readByteArray(KnowledgeSnapshot.SIGNATURE_BYTES);
        int mode = buffer.readVarInt();
        if (mode < 0 || mode >= KnowledgeShareWorkflow.Mode.values().length) {
            throw new IllegalArgumentException("Invalid Knowledge Book mode");
        }
        return new KnowledgeBookState(version, new KnowledgeSnapshot(
            KnowledgeSnapshot.CURRENT_VERSION, snapshotId, ownerId, issuedAt, expiresAt,
            knowledge, signature), KnowledgeShareWorkflow.Mode.values()[mode]);
    }
}
