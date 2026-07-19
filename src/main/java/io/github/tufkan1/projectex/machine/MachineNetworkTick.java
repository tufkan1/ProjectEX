package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One-tick conservative router with explicit cycle and work-budget rejection. */
public final class MachineNetworkTick {
    private final MachineTickBudget budget;
    private final Map<String, Set<String>> routes = new HashMap<>();
    private EmcValue transferred = EmcValue.ZERO;
    private int transferCount;

    public MachineNetworkTick(MachineTickBudget budget) {
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public Transfer route(
        String sourceId,
        MachineBuffer source,
        String targetId,
        MachineBuffer target,
        EmcValue requested
    ) {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(requested, "requested");
        if (sourceId.equals(targetId) || source == target || transferCount >= budget.maxTransfers()
            || pathExists(targetId, sourceId, new HashSet<>())) {
            return Transfer.rejected(requested, source.stored(), target.stored());
        }

        EmcValue budgetRemaining = budget.maxTransferredEmc().subtract(transferred);
        EmcValue movable = requested.min(source.stored())
            .min(target.capacity().subtract(target.stored()))
            .min(budgetRemaining);
        if (movable.equals(EmcValue.ZERO)) {
            return new Transfer(requested, EmcValue.ZERO, requested, source.stored(), target.stored(), true);
        }

        EmcValue extracted = source.extract(movable);
        EmcValue inserted = target.insert(extracted);
        if (!inserted.equals(extracted)) {
            throw new IllegalStateException("Validated machine transfer did not commit atomically");
        }
        routes.computeIfAbsent(sourceId, ignored -> new HashSet<>()).add(targetId);
        transferred = transferred.add(inserted);
        transferCount++;
        return new Transfer(
            requested,
            inserted,
            requested.subtract(inserted),
            source.stored(),
            target.stored(),
            true
        );
    }

    public EmcValue transferred() {
        return transferred;
    }

    public int transferCount() {
        return transferCount;
    }

    private boolean pathExists(String current, String destination, Set<String> visited) {
        if (current.equals(destination)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        return routes.getOrDefault(current, Set.of()).stream()
            .anyMatch(next -> pathExists(next, destination, visited));
    }

    public record Transfer(
        EmcValue requested,
        EmcValue moved,
        EmcValue remainder,
        EmcValue sourceStored,
        EmcValue targetStored,
        boolean allowed
    ) {
        public Transfer {
            Objects.requireNonNull(requested, "requested");
            Objects.requireNonNull(moved, "moved");
            Objects.requireNonNull(remainder, "remainder");
            Objects.requireNonNull(sourceStored, "sourceStored");
            Objects.requireNonNull(targetStored, "targetStored");
            if (!requested.equals(moved.add(remainder))) {
                throw new IllegalArgumentException("Machine transfer accounting does not balance");
            }
        }

        private static Transfer rejected(EmcValue requested, EmcValue source, EmcValue target) {
            return new Transfer(requested, EmcValue.ZERO, requested, source, target, false);
        }
    }
}
