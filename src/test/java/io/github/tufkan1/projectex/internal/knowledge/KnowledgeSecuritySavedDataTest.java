package io.github.tufkan1.projectex.internal.knowledge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.knowledge.KnowledgeReplayGuard;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshotSigner;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class KnowledgeSecuritySavedDataTest {
    @Test
    void signingSecretReplayLedgerAndAuditSurviveCodecReload() {
        KnowledgeSecuritySavedData original = new KnowledgeSecuritySavedData();
        UUID consumedId = UUID.randomUUID();
        original.replaceConsumed(Map.of(consumedId, 9_999L));
        original.audit("100 action=CONFIRM outcome=OK");
        var encoded = KnowledgeSecuritySavedData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        KnowledgeSecuritySavedData restored = KnowledgeSecuritySavedData.CODEC
            .parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertArrayEquals(original.secret(), restored.secret());
        assertEquals(original.consumed(), restored.consumed());
        assertEquals(original.auditEvents(), restored.auditEvents());

        Instant now = Instant.ofEpochSecond(1_000);
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(restored.secret());
        var snapshot = signer.create(UUID.randomUUID(), List.of(EmcKey.parse("minecraft:diamond")),
            now, Duration.ofHours(1), consumedId);
        KnowledgeReplayGuard replay = new KnowledgeReplayGuard(
            KnowledgeSecuritySavedData.MAX_REPLAYS, restored.consumed());
        assertFalse(replay.consume(snapshot, now), "Reloaded replay ledger accepted a consumed snapshot");
    }
}
