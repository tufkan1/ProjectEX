package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.content.component.KnowledgeBookState;
import io.github.tufkan1.projectex.knowledge.KnowledgeShareWorkflow;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshotSigner;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class KnowledgeBookStateTest {
    @Test
    void persistentCodecRoundTripsSignedSnapshotAndMode() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(new byte[32]);
        var snapshot = signer.create(UUID.randomUUID(), List.of(EmcKey.parse("minecraft:diamond")),
            Instant.ofEpochSecond(1_000), Duration.ofHours(1), UUID.randomUUID());
        KnowledgeBookState state = new KnowledgeBookState(
            KnowledgeBookState.CURRENT_VERSION, snapshot, KnowledgeShareWorkflow.Mode.REPLACE);
        var json = KnowledgeBookState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, KnowledgeBookState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void malformedSignatureFailsClosedDuringDecode() {
        String json = """
            {"version":1,"snapshot_id":"00000000-0000-0000-0000-000000000001",
             "owner_id":"00000000-0000-0000-0000-000000000002","issued_at":1,"expires_at":2,
             "knowledge":[],"signature":"not-base64!","mode":"merge"}
            """;
        assertThrows(IllegalArgumentException.class, () -> KnowledgeBookState.CODEC.parse(
            JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json)).getOrThrow());
    }
}
