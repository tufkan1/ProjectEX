package io.github.tufkan1.projectex.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class KnowledgeShareWorkflowTest {
    private static final Instant NOW = Instant.ofEpochSecond(20_000);
    private static final EmcKey COAL = new EmcKey("minecraft", "coal");
    private static final EmcKey DIAMOND = new EmcKey("minecraft", "diamond");
    private static final EmcKey EMERALD = new EmcKey("minecraft", "emerald");

    @Test
    void previewIsReadOnlyAndConfirmCommitsExactlyOnce() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(new byte[32]);
        KnowledgeShareWorkflow workflow = new KnowledgeShareWorkflow(signer, new KnowledgeReplayGuard(32));
        UUID recipient = UUID.randomUUID();
        PlayerAlchemyState current = state(COAL, DIAMOND);
        KnowledgeSnapshot snapshot = signer.create(UUID.randomUUID(), List.of(DIAMOND, EMERALD), NOW,
            Duration.ofHours(1), UUID.randomUUID());

        var preview = workflow.preview(snapshot, recipient, current, 7,
            KnowledgeShareWorkflow.Mode.MERGE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW);
        assertEquals(1, preview.preview().orElseThrow().added());
        assertEquals(1, preview.preview().orElseThrow().duplicates());
        assertEquals(2, current.knowledge().size(), "preview mutated current state");

        UUID token = preview.preview().orElseThrow().confirmationToken();
        var confirmed = workflow.confirm(token, recipient, current, 7, NOW);
        assertEquals(new TreeSet<>(List.of(COAL, DIAMOND, EMERALD)),
            confirmed.state().orElseThrow().knowledge());
        assertEquals(EmcValue.of(99), confirmed.state().orElseThrow().balance());
        assertEquals(KnowledgeShareWorkflow.Failure.UNKNOWN_OR_REPLAYED_CONFIRMATION,
            workflow.confirm(token, recipient, current, 7, NOW).failure());
    }

    @Test
    void replaceReportsRemovalAndRejectsWrongRecipientOrStaleState() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(new byte[32]);
        KnowledgeShareWorkflow workflow = new KnowledgeShareWorkflow(signer, new KnowledgeReplayGuard(32));
        UUID recipient = UUID.randomUUID();
        PlayerAlchemyState current = state(COAL, DIAMOND);
        KnowledgeSnapshot snapshot = signer.create(UUID.randomUUID(), List.of(EMERALD), NOW,
            Duration.ofHours(1), UUID.randomUUID());
        var preview = workflow.preview(snapshot, recipient, current, 4,
            KnowledgeShareWorkflow.Mode.REPLACE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW)
            .preview().orElseThrow();
        assertEquals(2, preview.removed());
        assertEquals(KnowledgeShareWorkflow.Failure.WRONG_RECIPIENT,
            workflow.confirm(preview.confirmationToken(), UUID.randomUUID(), current, 4, NOW).failure());

        var stale = workflow.preview(snapshot, recipient, current, 4,
            KnowledgeShareWorkflow.Mode.REPLACE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW)
            .preview().orElseThrow();
        assertEquals(KnowledgeShareWorkflow.Failure.STALE_STATE,
            workflow.confirm(stale.confirmationToken(), recipient, current, 5, NOW).failure());
    }

    @Test
    void policyAndConfirmationExpiryFailClosed() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(new byte[32]);
        KnowledgeShareWorkflow workflow = new KnowledgeShareWorkflow(signer, new KnowledgeReplayGuard(32));
        UUID recipient = UUID.randomUUID();
        PlayerAlchemyState current = state(COAL);
        KnowledgeSnapshot snapshot = signer.create(UUID.randomUUID(), List.of(EMERALD), NOW,
            Duration.ofHours(1), UUID.randomUUID());
        assertFalse(workflow.preview(snapshot, recipient, current, 1, KnowledgeShareWorkflow.Mode.MERGE,
            KnowledgeShareWorkflow.SharingPolicy.CREATIVE_ONLY, false, NOW).preview().isPresent());
        assertTrue(workflow.preview(snapshot, recipient, current, 1, KnowledgeShareWorkflow.Mode.MERGE,
            KnowledgeShareWorkflow.SharingPolicy.CREATIVE_ONLY, true, NOW).preview().isPresent());

        var expiring = workflow.preview(snapshot, recipient, current, 1,
            KnowledgeShareWorkflow.Mode.MERGE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW)
            .preview().orElseThrow();
        assertEquals(KnowledgeShareWorkflow.Failure.UNKNOWN_OR_REPLAYED_CONFIRMATION,
            workflow.confirm(expiring.confirmationToken(), recipient, current, 1,
                NOW.plusSeconds(KnowledgeShareWorkflow.CONFIRMATION_SECONDS + 1)).failure());
    }

    @Test
    void cancellationAndDisconnectInvalidatePendingTokensWithoutMutation() {
        KnowledgeSnapshotSigner signer = new KnowledgeSnapshotSigner(new byte[32]);
        KnowledgeShareWorkflow workflow = new KnowledgeShareWorkflow(signer, new KnowledgeReplayGuard(32));
        UUID recipient = UUID.randomUUID();
        PlayerAlchemyState current = state(COAL);
        KnowledgeSnapshot snapshot = signer.create(UUID.randomUUID(), List.of(EMERALD), NOW,
            Duration.ofHours(1), UUID.randomUUID());
        var cancelled = workflow.preview(snapshot, recipient, current, 1,
            KnowledgeShareWorkflow.Mode.MERGE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW)
            .preview().orElseThrow();
        assertTrue(workflow.cancel(cancelled.confirmationToken(), recipient));
        assertEquals(KnowledgeShareWorkflow.Failure.UNKNOWN_OR_REPLAYED_CONFIRMATION,
            workflow.confirm(cancelled.confirmationToken(), recipient, current, 1, NOW).failure());

        var disconnected = workflow.preview(snapshot, recipient, current, 1,
            KnowledgeShareWorkflow.Mode.MERGE, KnowledgeShareWorkflow.SharingPolicy.ENABLED, false, NOW)
            .preview().orElseThrow();
        workflow.cancelRecipient(recipient);
        assertEquals(KnowledgeShareWorkflow.Failure.UNKNOWN_OR_REPLAYED_CONFIRMATION,
            workflow.confirm(disconnected.confirmationToken(), recipient, current, 1, NOW).failure());
        assertEquals(new TreeSet<>(List.of(COAL)), current.knowledge());
    }

    private static PlayerAlchemyState state(EmcKey... keys) {
        return new PlayerAlchemyState(EmcValue.of(99), new TreeSet<>(List.of(keys)));
    }
}
