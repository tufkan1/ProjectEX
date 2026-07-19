package io.github.tufkan1.projectex.network;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.teleport.AlchemicalTeleportContext;
import io.github.tufkan1.projectex.api.teleport.AlchemicalTeleportProtection;
import io.github.tufkan1.projectex.content.AlchemicalBookItem;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.component.AlchemicalBookState;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import io.github.tufkan1.projectex.internal.teleport.AlchemicalBookSavedData;
import io.github.tufkan1.projectex.teleport.AlchemicalBookConfig;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalBookTier;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;

/** Server-created, exact-stack sessions for all Alchemical Book mutations and teleports. */
public final class AlchemicalBookSessionRegistry {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final NetworkRequestLimiter limiter = new NetworkRequestLimiter();

    public synchronized AlchemicalBookViewPayload open(
        ServerPlayer player, InteractionHand hand, ItemStack stack, AlchemicalBookTier tier
    ) {
        close(player.getUUID());
        UUID sessionId = UUID.randomUUID();
        AlchemicalBookState state = stack.getOrDefault(
            ProjectEXComponents.ALCHEMICAL_BOOK_STATE, AlchemicalBookState.EMPTY);
        Session session = new Session(sessionId, player.getUUID(), hand, stack, tier, state);
        sessions.put(player.getUUID(), session);
        ResolvedStore store = resolve(player, session);
        if (store.failure != null) {
            sessions.remove(player.getUUID());
            return view(player, session, -1, false, AlchemicalBookLocations.EMPTY, store.failure);
        }
        session.boundRevision = store.revision;
        return view(player, session, -1, store.editable, store.locations, "");
    }

    public synchronized AlchemicalBookViewPayload handle(
        ServerPlayer player, AlchemicalBookActionPayload payload, long monotonicMillis
    ) {
        Session session = sessions.get(player.getUUID());
        if (!limiter.allow(player.getUUID(), monotonicMillis)) return failure(player, payload, session, "rate_limited");
        if (payload.protocolVersion() != AlchemicalBookActionPayload.PROTOCOL_VERSION) {
            return failure(player, payload, session, "unsupported_protocol");
        }
        if (session == null || !session.id.equals(payload.sessionId()) || !authorized(player, session)) {
            close(player.getUUID());
            return failure(player, payload, session, "session_invalid");
        }
        if (payload.requestId() < 0 || payload.requestId() <= session.lastRequestId) {
            return failure(player, payload, session, "replayed_request");
        }
        session.lastRequestId = payload.requestId();
        if (payload.action() < 0 || payload.action() >= AlchemicalBookAction.values().length) {
            return failure(player, payload, session, "malformed_request");
        }
        AlchemicalBookAction action = AlchemicalBookAction.values()[payload.action()];
        if (action == AlchemicalBookAction.CLOSE) {
            close(player.getUUID());
            return view(player, session, payload.requestId(), false, AlchemicalBookLocations.EMPTY, "");
        }
        ResolvedStore store = resolve(player, session);
        if (store.failure != null) return view(player, session, payload.requestId(), false,
            AlchemicalBookLocations.EMPTY, store.failure);
        if (session.state.owner().isPresent() && store.revision != session.boundRevision) {
            return view(player, session, payload.requestId(), store.editable, store.locations, "state_changed");
        }
        try {
            String failure = switch (action) {
                case CREATE -> create(player, session, store, payload.name());
                case DELETE -> delete(player, session, store, payload.name());
                case TELEPORT -> teleport(player, session, store, payload.name(), false);
                case BACK -> teleport(player, session, store, "", true);
                case CLOSE -> throw new IllegalStateException("Close handled earlier");
            };
            ResolvedStore refreshed = resolve(player, session);
            String result = failure.isEmpty() && refreshed.failure != null ? refreshed.failure : failure;
            AlchemicalBookLocations locations = refreshed.failure == null ? refreshed.locations : store.locations;
            boolean editable = refreshed.failure == null && refreshed.editable;
            audit(player, action, result.isEmpty(), result);
            return view(player, session, payload.requestId(), editable, locations, result);
        } catch (IllegalArgumentException exception) {
            String reason = switch (action) {
                case CREATE -> store.locations.find(payload.name()).isPresent() ? "duplicate_name" : "invalid_name";
                case DELETE, TELEPORT -> "name_not_found";
                case BACK -> "no_back_location";
                default -> "malformed_request";
            };
            audit(player, action, false, reason);
            return view(player, session, payload.requestId(), store.editable, store.locations, reason);
        }
    }

    public synchronized void close(UUID playerId) {
        sessions.remove(playerId);
        limiter.disconnect(playerId);
    }

    private String create(ServerPlayer player, Session session, ResolvedStore store, String name) {
        if (!store.editable) return "edit_not_allowed";
        String safeName = AlchemicalDestination.validateName(name);
        BlockPos pos = player.blockPosition();
        AlchemicalDestination destination = new AlchemicalDestination(safeName,
            player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
        return update(session, store, store.locations.add(destination)) ? "" : "state_changed";
    }

    private String delete(ServerPlayer player, Session session, ResolvedStore store, String name) {
        if (!store.editable) return "edit_not_allowed";
        return update(session, store, store.locations.remove(name)) ? "" : "state_changed";
    }

    private String teleport(
        ServerPlayer player, Session session, ResolvedStore store, String name, boolean back
    ) {
        AlchemicalDestination destination = back
            ? store.locations.back().orElseThrow(() -> new IllegalArgumentException("No back target"))
            : store.locations.find(name).orElseThrow(() -> new IllegalArgumentException("Unknown target"));
        boolean differentDimension = !player.level().dimension().equals(destination.dimensionKey());
        if (differentDimension && !session.tier.crossDimension()) return "wrong_dimension";
        ServerLevel target = player.level().getServer().getLevel(destination.dimensionKey());
        if (target == null) return "dimension_not_found";
        BlockPos targetPos = destination.pos();
        if (!target.isInWorldBounds(targetPos) || !target.getWorldBorder().isWithinBounds(targetPos)) {
            return "unsafe_destination";
        }
        BlockPos origin = player.blockPosition();
        if (!AlchemicalTeleportProtection.EVENT.invoker().canTeleport(new AlchemicalTeleportContext(
            player, session.stack, session.tier, player.level(), origin, target, targetPos))) {
            return "protected_destination";
        }
        AlchemicalDestination previous = new AlchemicalDestination("Previous location",
            player.level().dimension().identifier().toString(), origin.getX(), origin.getY(), origin.getZ());
        EmcValue cost = session.tier.cost(origin, destination, player.getAbilities().instabuild);
        if (!cost.equals(EmcValue.ZERO) && !MatterEmcPayment.debit(player, cost)) return "not_enough_emc";
        boolean moved = player.teleportTo(target, destination.x() + 0.5, destination.y(), destination.z() + 0.5,
            Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
        if (!moved) {
            if (!cost.equals(EmcValue.ZERO)) MatterEmcPayment.credit(player, cost);
            return "teleport_failed";
        }
        AlchemicalBookLocations replacement = back ? store.locations.clearBack() : store.locations.withBack(previous);
        return update(session, store, replacement) ? "" : "state_changed";
    }

    private boolean update(Session session, ResolvedStore store, AlchemicalBookLocations replacement) {
        if (session.state.owner().isPresent()) {
            UUID owner = session.state.owner().orElseThrow();
            AlchemicalBookSavedData data = AlchemicalBookSavedData.get(store.player.level().getServer());
            boolean updated = data.compareAndSet(owner, store.locations, store.revision, replacement);
            if (updated) session.boundRevision = data.revision(owner);
            return updated;
        }
        AlchemicalBookState live = session.stack.get(ProjectEXComponents.ALCHEMICAL_BOOK_STATE);
        if (!session.state.equals(live)) return false;
        session.state = session.state.withStackLocations(replacement);
        session.stack.set(ProjectEXComponents.ALCHEMICAL_BOOK_STATE, session.state);
        return true;
    }

    private ResolvedStore resolve(ServerPlayer actor, Session session) {
        AlchemicalBookState live = session.stack.get(ProjectEXComponents.ALCHEMICAL_BOOK_STATE);
        if (live == null || !live.equals(session.state)) return ResolvedStore.failure(actor, "state_changed");
        if (live.owner().isEmpty()) return new ResolvedStore(actor, live.stackLocations(), 0, true, null);
        UUID ownerId = live.owner().orElseThrow();
        ServerPlayer owner = actor.level().getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return ResolvedStore.failure(actor, "owner_offline");
        AlchemicalBookSavedData data = AlchemicalBookSavedData.get(actor.level().getServer());
        boolean editable = ownerId.equals(actor.getUUID())
            || AlchemicalBookConfig.policy() == AlchemicalBookConfig.EditPolicy.ENABLED
            || AlchemicalBookConfig.policy() == AlchemicalBookConfig.EditPolicy.OPERATOR_ONLY
                && actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        return new ResolvedStore(actor, data.locations(ownerId), data.revision(ownerId), editable, null);
    }

    private static boolean authorized(ServerPlayer player, Session session) {
        if (player.hasDisconnected() || player.getItemInHand(session.hand) != session.stack) return false;
        return session.stack.getItem() instanceof AlchemicalBookItem book && book.tier() == session.tier;
    }

    private static AlchemicalBookViewPayload failure(
        ServerPlayer player, AlchemicalBookActionPayload payload, Session session, String reason
    ) {
        AlchemicalBookTier tier = session == null ? AlchemicalBookTier.BASIC : session.tier;
        return new AlchemicalBookViewPayload(payload.sessionId(), Math.max(-1, payload.requestId()), tier.ordinal(),
            false, MatterEmcPayment.balance(player).amount().toString(), reason, java.util.List.of(), Optional.empty());
    }

    private static AlchemicalBookViewPayload view(
        ServerPlayer player, Session session, long requestId, boolean editable,
        AlchemicalBookLocations locations, String failure
    ) {
        ArrayList<AlchemicalBookViewPayload.Entry> entries = new ArrayList<>();
        for (AlchemicalDestination destination : locations.destinations()) {
            entries.add(new AlchemicalBookViewPayload.Entry(destination,
                session.tier.cost(player.blockPosition(), destination, player.getAbilities().instabuild)
                    .amount().toString()));
        }
        Optional<AlchemicalBookViewPayload.Entry> back = locations.back().map(destination ->
            new AlchemicalBookViewPayload.Entry(destination,
                session.tier.cost(player.blockPosition(), destination, player.getAbilities().instabuild)
                    .amount().toString()));
        return new AlchemicalBookViewPayload(session.id, requestId, session.tier.ordinal(), editable,
            MatterEmcPayment.balance(player).amount().toString(), failure, entries, back);
    }

    private static void audit(ServerPlayer player, AlchemicalBookAction action, boolean success, String failure) {
        ProjectEX.LOGGER.info("Alchemical Book actor={} action={} success={} failure={}",
            player.getUUID(), action, success, failure);
    }

    private static final class Session {
        private final UUID id;
        private final UUID playerId;
        private final InteractionHand hand;
        private final ItemStack stack;
        private final AlchemicalBookTier tier;
        private AlchemicalBookState state;
        private long boundRevision;
        private long lastRequestId = -1;
        private Session(UUID id, UUID playerId, InteractionHand hand, ItemStack stack,
                        AlchemicalBookTier tier, AlchemicalBookState state) {
            this.id = id; this.playerId = playerId; this.hand = hand; this.stack = stack;
            this.tier = tier; this.state = state;
        }
    }

    private record ResolvedStore(
        ServerPlayer player, AlchemicalBookLocations locations, long revision, boolean editable, String failure
    ) {
        private static ResolvedStore failure(ServerPlayer player, String reason) {
            return new ResolvedStore(player, AlchemicalBookLocations.EMPTY, 0, false, reason);
        }
    }
}
