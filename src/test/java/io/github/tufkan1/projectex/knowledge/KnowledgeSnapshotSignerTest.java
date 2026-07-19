package io.github.tufkan1.projectex.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class KnowledgeSnapshotSignerTest {
    private static final byte[] SECRET = new byte[32];
    private static final Instant NOW = Instant.ofEpochSecond(10_000);

    @Test
    void canonicalSignedSnapshotRejectsTamperingAndExpiry() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(SECRET);
        UUID owner = UUID.randomUUID();
        KnowledgeSnapshot snapshot = signer.create(owner, List.of(
            new EmcKey("minecraft", "diamond"), new EmcKey("minecraft", "coal")
        ), NOW, Duration.ofHours(1), UUID.randomUUID());

        assertEquals(KnowledgeSnapshotSigner.Verification.VALID, signer.verify(snapshot, NOW));
        assertEquals(List.of(new EmcKey("minecraft", "coal"), new EmcKey("minecraft", "diamond")),
            snapshot.knowledge());
        KnowledgeSnapshot tampered = new KnowledgeSnapshot(
            snapshot.version(), snapshot.snapshotId(), UUID.randomUUID(), snapshot.issuedAt(),
            snapshot.expiresAt(), snapshot.knowledge(), snapshot.signature()
        );
        assertEquals(KnowledgeSnapshotSigner.Verification.INVALID_SIGNATURE, signer.verify(tampered, NOW));
        assertEquals(KnowledgeSnapshotSigner.Verification.EXPIRED,
            signer.verify(snapshot, NOW.plus(Duration.ofHours(1))));
    }

    @Test
    void lifetimeSizeAndSecretLimitsFailBeforeSigning() {
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeSnapshotSigner(new byte[31]));
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(SECRET);
        assertThrows(IllegalArgumentException.class, () -> signer.create(
            UUID.randomUUID(), List.of(), NOW, Duration.ofDays(8), UUID.randomUUID()
        ));
        List<EmcKey> oversized = java.util.stream.IntStream.rangeClosed(0, KnowledgeSnapshot.MAX_ENTRIES)
            .mapToObj(index -> new EmcKey("test", "item_" + index)).toList();
        assertThrows(IllegalArgumentException.class, () -> signer.create(
            UUID.randomUUID(), oversized, NOW, Duration.ofHours(1), UUID.randomUUID()
        ));
    }

    @Test
    void replayGuardIsOneShotAndBounded() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(SECRET);
        KnowledgeReplayGuard guard = new KnowledgeReplayGuard(2);
        KnowledgeSnapshot first = signer.create(UUID.randomUUID(), List.of(), NOW,
            Duration.ofHours(1), UUID.randomUUID());
        assertTrue(guard.consume(first, NOW));
        assertFalse(guard.consume(first, NOW));
        assertTrue(guard.consume(signer.create(UUID.randomUUID(), List.of(), NOW,
            Duration.ofHours(1), UUID.randomUUID()), NOW));
        assertTrue(guard.consume(signer.create(UUID.randomUUID(), List.of(), NOW,
            Duration.ofHours(1), UUID.randomUUID()), NOW));
        assertEquals(2, guard.size());
    }
}
