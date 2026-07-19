package io.github.tufkan1.projectex.knowledge;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** Two-phase preview/confirm boundary; preview never returns a mutable player state. */
public final class KnowledgeShareWorkflow {
    public static final int MAX_PENDING = 1_024;
    public static final long CONFIRMATION_SECONDS = 120;

    private final KnowledgeSnapshotSigner signer;
    private final KnowledgeReplayGuard replayGuard;
    private final LinkedHashMap<UUID, Pending> pending = new LinkedHashMap<>();

    public KnowledgeShareWorkflow(KnowledgeSnapshotSigner signer, KnowledgeReplayGuard replayGuard) {
        this.signer = java.util.Objects.requireNonNull(signer, "signer");
        this.replayGuard = java.util.Objects.requireNonNull(replayGuard, "replayGuard");
    }

    public synchronized PreviewResult preview(
        KnowledgeSnapshot snapshot, UUID recipient, PlayerAlchemyState current, long stateRevision,
        Mode mode, SharingPolicy policy, boolean creative, Instant now
    ) {
        prune(now);
        if (policy == SharingPolicy.DISABLED || policy == SharingPolicy.CREATIVE_ONLY && !creative) {
            return PreviewResult.failure(Failure.POLICY_DENIED);
        }
        if (signer.verify(snapshot, now) != KnowledgeSnapshotSigner.Verification.VALID) {
            return PreviewResult.failure(Failure.INVALID_SNAPSHOT);
        }
        SortedSet<EmcKey> result = new TreeSet<>();
        if (mode == Mode.MERGE) result.addAll(current.knowledge());
        result.addAll(snapshot.knowledge());
        if (result.size() > PlayerAlchemyState.MAX_KNOWLEDGE_ENTRIES) {
            return PreviewResult.failure(Failure.OVERSIZED_RESULT);
        }
        TreeSet<EmcKey> added = new TreeSet<>(result);
        added.removeAll(current.knowledge());
        TreeSet<EmcKey> removed = new TreeSet<>(current.knowledge());
        removed.removeAll(result);
        TreeSet<EmcKey> duplicates = new TreeSet<>(current.knowledge());
        duplicates.retainAll(snapshot.knowledge());
        UUID token = UUID.randomUUID();
        long expiresAt = Math.min(snapshot.expiresAt(), now.getEpochSecond() + CONFIRMATION_SECONDS);
        while (pending.size() >= MAX_PENDING) pending.remove(pending.keySet().iterator().next());
        pending.put(token, new Pending(snapshot, recipient, stateRevision, mode, expiresAt));
        return new PreviewResult(Optional.of(new Preview(
            token, snapshot.ownerId(), mode, added.size(), removed.size(), duplicates.size(), result.size(), expiresAt
        )), Failure.NONE);
    }

    public synchronized ConfirmResult confirm(
        UUID token, UUID recipient, PlayerAlchemyState current, long stateRevision, Instant now
    ) {
        prune(now);
        Pending request = pending.remove(token);
        if (request == null) return ConfirmResult.failure(Failure.UNKNOWN_OR_REPLAYED_CONFIRMATION);
        if (!request.recipient.equals(recipient)) return ConfirmResult.failure(Failure.WRONG_RECIPIENT);
        if (request.stateRevision != stateRevision) return ConfirmResult.failure(Failure.STALE_STATE);
        if (request.expiresAt <= now.getEpochSecond()) return ConfirmResult.failure(Failure.EXPIRED_CONFIRMATION);
        if (signer.verify(request.snapshot, now) != KnowledgeSnapshotSigner.Verification.VALID) {
            return ConfirmResult.failure(Failure.INVALID_SNAPSHOT);
        }
        TreeSet<EmcKey> result = new TreeSet<>();
        if (request.mode == Mode.MERGE) result.addAll(current.knowledge());
        result.addAll(request.snapshot.knowledge());
        if (result.size() > PlayerAlchemyState.MAX_KNOWLEDGE_ENTRIES) {
            return ConfirmResult.failure(Failure.OVERSIZED_RESULT);
        }
        if (!replayGuard.consume(request.snapshot, now)) {
            return ConfirmResult.failure(Failure.SNAPSHOT_REPLAYED);
        }
        return new ConfirmResult(Optional.of(new PlayerAlchemyState(current.balance(), result)), Failure.NONE);
    }

    public synchronized boolean cancel(UUID token, UUID recipient) {
        Pending request = pending.get(token);
        if (request == null || !request.recipient.equals(recipient)) return false;
        pending.remove(token);
        return true;
    }

    public synchronized void cancelRecipient(UUID recipient) {
        pending.entrySet().removeIf(entry -> entry.getValue().recipient.equals(recipient));
    }

    private void prune(Instant now) {
        pending.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now.getEpochSecond());
    }

    public enum Mode { MERGE, REPLACE }
    public enum SharingPolicy { ENABLED, CREATIVE_ONLY, DISABLED }
    public enum Failure {
        NONE, POLICY_DENIED, INVALID_SNAPSHOT, OVERSIZED_RESULT, UNKNOWN_OR_REPLAYED_CONFIRMATION,
        WRONG_RECIPIENT, STALE_STATE, EXPIRED_CONFIRMATION, SNAPSHOT_REPLAYED
    }

    public record Preview(
        UUID confirmationToken, UUID ownerId, Mode mode, int added, int removed, int duplicates,
        int resultSize, long expiresAt
    ) {}
    public record PreviewResult(Optional<Preview> preview, Failure failure) {
        private static PreviewResult failure(Failure failure) { return new PreviewResult(Optional.empty(), failure); }
    }
    public record ConfirmResult(Optional<PlayerAlchemyState> state, Failure failure) {
        private static ConfirmResult failure(Failure failure) { return new ConfirmResult(Optional.empty(), failure); }
    }
    private record Pending(
        KnowledgeSnapshot snapshot, UUID recipient, long stateRevision, Mode mode, long expiresAt
    ) {}
}
