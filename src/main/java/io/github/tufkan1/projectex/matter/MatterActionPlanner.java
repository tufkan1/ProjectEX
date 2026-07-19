package io.github.tufkan1.projectex.matter;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/** Deterministic server plan for bounded area actions; world mutation occurs only after this succeeds. */
public final class MatterActionPlanner {
    private MatterActionPlanner() {
    }

    public static Plan plan(
        MatterTier tier,
        UUID actor,
        Position raycastOrigin,
        List<Position> candidates,
        Predicate<Position> protection,
        EmcValue availableEmc,
        int charge
    ) {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(raycastOrigin, "raycastOrigin");
        Objects.requireNonNull(protection, "protection");
        Objects.requireNonNull(availableEmc, "availableEmc");
        int radius = tier.radiusForCharge(charge);
        List<Position> ordered = candidates.stream().distinct()
            .filter(position -> position.chebyshevDistance(raycastOrigin) <= radius)
            .sorted(Comparator.comparingInt((Position position) -> position.squaredDistance(raycastOrigin))
                .thenComparing(Position::compareTo))
            .limit(tier.maxAreaBlocks())
            .toList();

        List<Position> accepted = new ArrayList<>();
        List<Position> denied = new ArrayList<>();
        EmcValue cost = EmcValue.ZERO;
        for (Position position : ordered) {
            if (!protection.test(position)) {
                denied.add(position);
                continue;
            }
            EmcValue next = cost.add(tier.emcPerAreaBlock());
            if (next.compareTo(availableEmc) > 0) break;
            accepted.add(position);
            cost = next;
        }
        return new Plan(actor, tier.id(), raycastOrigin, accepted, denied, cost,
            accepted.isEmpty() ? 0 : tier.actionCooldownTicks());
    }

    public record Position(int x, int y, int z) implements Comparable<Position> {
        int chebyshevDistance(Position other) {
            return Math.max(Math.max(Math.abs(x - other.x), Math.abs(y - other.y)), Math.abs(z - other.z));
        }
        int squaredDistance(Position other) {
            int dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
        @Override public int compareTo(Position other) {
            int xOrder = Integer.compare(x, other.x);
            if (xOrder != 0) return xOrder;
            int yOrder = Integer.compare(y, other.y);
            return yOrder != 0 ? yOrder : Integer.compare(z, other.z);
        }
    }

    public record Plan(
        UUID actor,
        String tier,
        Position origin,
        List<Position> accepted,
        List<Position> protectionDenied,
        EmcValue emcCost,
        int cooldownTicks
    ) {
        public Plan {
            accepted = List.copyOf(accepted);
            protectionDenied = List.copyOf(protectionDenied);
            if (cooldownTicks < 0) throw new IllegalArgumentException("Negative cooldown");
        }
    }
}
