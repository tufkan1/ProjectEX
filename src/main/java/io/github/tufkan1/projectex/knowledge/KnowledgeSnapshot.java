package io.github.tufkan1.projectex.knowledge;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Canonical, owner-bound knowledge payload stored by sharing items. */
public record KnowledgeSnapshot(
    int version, UUID snapshotId, UUID ownerId, long issuedAt, long expiresAt,
    List<EmcKey> knowledge, byte[] signature
) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_ENTRIES = 4_096;
    public static final int SIGNATURE_BYTES = 32;

    public KnowledgeSnapshot {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported knowledge snapshot");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(ownerId, "ownerId");
        if (issuedAt < 0 || expiresAt <= issuedAt) throw new IllegalArgumentException("Invalid snapshot lifetime");
        knowledge = List.copyOf(knowledge).stream().distinct().sorted().toList();
        if (knowledge.size() > MAX_ENTRIES) throw new IllegalArgumentException("Knowledge snapshot is oversized");
        signature = Arrays.copyOf(signature, signature.length);
        if (signature.length != 0 && signature.length != SIGNATURE_BYTES) {
            throw new IllegalArgumentException("Invalid knowledge snapshot signature");
        }
    }

    @Override public byte[] signature() { return Arrays.copyOf(signature, signature.length); }

    public KnowledgeSnapshot withSignature(byte[] value) {
        return new KnowledgeSnapshot(version, snapshotId, ownerId, issuedAt, expiresAt, knowledge, value);
    }

    public byte[] canonicalBytes() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(version);
            output.writeLong(snapshotId.getMostSignificantBits());
            output.writeLong(snapshotId.getLeastSignificantBits());
            output.writeLong(ownerId.getMostSignificantBits());
            output.writeLong(ownerId.getLeastSignificantBits());
            output.writeLong(issuedAt);
            output.writeLong(expiresAt);
            output.writeInt(knowledge.size());
            for (EmcKey key : knowledge) output.writeUTF(key.toString());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode knowledge snapshot", exception);
        }
    }
}
