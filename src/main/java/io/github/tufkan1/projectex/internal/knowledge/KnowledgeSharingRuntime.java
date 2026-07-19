package io.github.tufkan1.projectex.internal.knowledge;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.component.KnowledgeBookState;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.knowledge.KnowledgeReplayGuard;
import io.github.tufkan1.projectex.knowledge.KnowledgeShareWorkflow;
import io.github.tufkan1.projectex.knowledge.KnowledgeSharingAccess;
import io.github.tufkan1.projectex.knowledge.KnowledgeSharingConfig;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshot;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshotSigner;
import io.github.tufkan1.projectex.network.KnowledgeSharePreviewPayload;
import io.github.tufkan1.projectex.network.KnowledgeShareResultPayload;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-owned adapter that commits the loader-neutral two-phase workflow atomically. */
public final class KnowledgeSharingRuntime {
    private static final Map<MinecraftServer, KnowledgeSharingRuntime> INSTANCES = new WeakHashMap<>();

    private final KnowledgeSecuritySavedData security;
    private final KnowledgeSnapshotSigner signer;
    private final KnowledgeReplayGuard replay;
    private final KnowledgeShareWorkflow workflow;
    private final LinkedHashMap<UUID, Binding> bindings = new LinkedHashMap<>();

    private KnowledgeSharingRuntime(MinecraftServer server) {
        security = KnowledgeSecuritySavedData.get(server);
        signer = new KnowledgeSnapshotSigner(security.secret());
        replay = new KnowledgeReplayGuard(KnowledgeSecuritySavedData.MAX_REPLAYS, security.consumed());
        workflow = new KnowledgeShareWorkflow(signer, replay);
    }

    public static synchronized KnowledgeSharingRuntime get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, KnowledgeSharingRuntime::new);
    }

    public KnowledgeSnapshot capture(ServerPlayer owner, Instant now) {
        PlayerAlchemyState state = PlayerAlchemySavedData.get(owner.level().getServer()).state(owner.getUUID());
        KnowledgeSnapshot snapshot = signer.create(owner.getUUID(), state.knowledge(), now,
            KnowledgeSharingConfig.snapshot().lifetime(), UUID.randomUUID());
        audit(now, "CAPTURE", owner.getUUID(), owner.getUUID(), snapshot.snapshotId(), "OK");
        return snapshot;
    }

    public synchronized PreviewResponse preview(ServerPlayer recipient, KnowledgeBookState book, Instant now) {
        KnowledgeSnapshot snapshot = book.snapshot();
        if (recipient.getUUID().equals(snapshot.ownerId())) return failure("SELF_SHARE", recipient, snapshot, now);
        if (!KnowledgeSharingAccess.boundary().permits(recipient, snapshot.ownerId())) {
            return failure("BOUNDARY_DENIED", recipient, snapshot, now);
        }
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(recipient.level().getServer());
        PlayerAlchemyState current = data.state(recipient.getUUID());
        KnowledgeShareWorkflow.PreviewResult result = workflow.preview(
            snapshot, recipient.getUUID(), current, data.revision(recipient.getUUID()), book.mode(),
            KnowledgeSharingConfig.snapshot().policy(), recipient.getAbilities().instabuild, now);
        if (result.preview().isEmpty()) return failure(result.failure().name(), recipient, snapshot, now);
        KnowledgeShareWorkflow.Preview preview = result.preview().orElseThrow();
        while (bindings.size() >= KnowledgeShareWorkflow.MAX_PENDING) {
            bindings.remove(bindings.keySet().iterator().next());
        }
        bindings.put(preview.confirmationToken(), new Binding(
            recipient.getUUID(), snapshot.ownerId(), snapshot.snapshotId()));
        audit(now, "PREVIEW", recipient.getUUID(), snapshot.ownerId(), snapshot.snapshotId(),
            book.mode() + ":+" + preview.added() + ":-" + preview.removed() + ":dup" + preview.duplicates());
        return new PreviewResponse(Optional.of(new KnowledgeSharePreviewPayload(
            preview.confirmationToken(), preview.ownerId(), preview.mode().ordinal(), preview.added(),
            preview.removed(), preview.duplicates(), preview.resultSize(), preview.expiresAt())), Optional.empty());
    }

    public synchronized KnowledgeShareResultPayload decide(
        ServerPlayer recipient, UUID token, boolean accepted, Instant now
    ) {
        Binding binding = bindings.remove(token);
        UUID snapshotId = binding == null ? null : binding.snapshotId;
        UUID ownerId = binding == null ? null : binding.ownerId;
        if (!accepted) {
            boolean cancelled = workflow.cancel(token, recipient.getUUID());
            audit(now, "CANCEL", recipient.getUUID(), ownerId, snapshotId, cancelled ? "OK" : "UNKNOWN_TOKEN");
            return new KnowledgeShareResultPayload(false, cancelled ? "cancelled" : "unknown_token", 0, 0);
        }
        if (binding == null || !binding.recipient.equals(recipient.getUUID()) || !holds(recipient, snapshotId)) {
            workflow.cancel(token, recipient.getUUID());
            audit(now, "CONFIRM", recipient.getUUID(), ownerId, snapshotId, "BOOK_MISSING");
            return new KnowledgeShareResultPayload(false, "book_missing", 0, 0);
        }
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(recipient.level().getServer());
        PlayerAlchemyState before = data.state(recipient.getUUID());
        long revision = data.revision(recipient.getUUID());
        KnowledgeShareWorkflow.ConfirmResult result = workflow.confirm(
            token, recipient.getUUID(), before, revision, now);
        security.replaceConsumed(replay.snapshot(now));
        if (result.state().isEmpty()) {
            audit(now, "CONFIRM", recipient.getUUID(), ownerId, snapshotId, result.failure().name());
            return new KnowledgeShareResultPayload(false, result.failure().name().toLowerCase(java.util.Locale.ROOT), 0,
                before.knowledge().size());
        }
        PlayerAlchemyState after = result.state().orElseThrow();
        if (!data.compareAndSet(recipient.getUUID(), before, revision, after)) {
            audit(now, "CONFIRM", recipient.getUUID(), ownerId, snapshotId, "STALE_COMMIT");
            return new KnowledgeShareResultPayload(false, "stale_commit", 0, before.knowledge().size());
        }
        int learned = Math.max(0, after.knowledge().size() - before.knowledge().size());
        audit(now, "CONFIRM", recipient.getUUID(), ownerId, snapshotId,
            "OK:+" + learned + ":total" + after.knowledge().size());
        return new KnowledgeShareResultPayload(true, "confirmed", learned, after.knowledge().size());
    }

    public synchronized void disconnect(UUID recipient) {
        boolean hadPending = bindings.values().stream().anyMatch(binding -> binding.recipient.equals(recipient));
        workflow.cancelRecipient(recipient);
        bindings.entrySet().removeIf(entry -> entry.getValue().recipient.equals(recipient));
        if (hadPending) audit(Instant.now(), "DISCONNECT", recipient, null, null, "PENDING_CANCELLED");
    }

    private PreviewResponse failure(
        String reason, ServerPlayer recipient, KnowledgeSnapshot snapshot, Instant now
    ) {
        audit(now, "PREVIEW", recipient.getUUID(), snapshot.ownerId(), snapshot.snapshotId(), reason);
        return new PreviewResponse(Optional.empty(), Optional.of(
            new KnowledgeShareResultPayload(false, reason.toLowerCase(java.util.Locale.ROOT), 0, 0)));
    }

    private static boolean holds(ServerPlayer player, UUID snapshotId) {
        return java.util.stream.Stream.of(player.getMainHandItem(), player.getOffhandItem()).anyMatch(stack -> {
            KnowledgeBookState state = stack.get(ProjectEXComponents.KNOWLEDGE_BOOK_STATE);
            return state != null && state.snapshot().snapshotId().equals(snapshotId);
        });
    }

    private void audit(Instant now, String action, UUID actor, UUID owner, UUID snapshot, String outcome) {
        String event = now.getEpochSecond() + " action=" + action + " actor=" + actor
            + " owner=" + owner + " snapshot=" + snapshot + " outcome=" + outcome;
        security.audit(event);
        ProjectEX.LOGGER.info("Knowledge share {}", event);
    }

    public record PreviewResponse(
        Optional<KnowledgeSharePreviewPayload> preview, Optional<KnowledgeShareResultPayload> failure
    ) { }

    private record Binding(UUID recipient, UUID ownerId, UUID snapshotId) { }
}
