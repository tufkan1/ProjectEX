package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Atomically published, immutable star-capacity snapshot. */
public final class KleinStarTierConfig {
    private static final Map<KleinStarTier, EmcValue> DEFAULTS = defaults();
    private static final AtomicReference<Map<KleinStarTier, EmcValue>> SNAPSHOT =
        new AtomicReference<>(DEFAULTS);

    private KleinStarTierConfig() { }

    public static EmcValue capacity(KleinStarTier tier) {
        return SNAPSHOT.get().getOrDefault(tier, tier.defaultCapacity());
    }

    public static Map<KleinStarTier, EmcValue> snapshot() { return SNAPSHOT.get(); }

    public static void publish(Map<KleinStarTier, EmcValue> capacities) {
        if (capacities.size() != KleinStarTier.values().length) {
            throw new IllegalArgumentException("Star tier snapshot must define every registered tier");
        }
        EnumMap<KleinStarTier, EmcValue> copy = new EnumMap<>(KleinStarTier.class);
        copy.putAll(capacities);
        EmcValue previous = EmcValue.ZERO;
        for (KleinStarTier tier : KleinStarTier.values()) {
            EmcValue capacity = copy.get(tier);
            if (capacity == null || capacity.compareTo(previous) <= 0) {
                throw new IllegalArgumentException("Star capacities must be positive and strictly increasing");
            }
            previous = capacity;
        }
        SNAPSHOT.set(Map.copyOf(copy));
    }

    private static Map<KleinStarTier, EmcValue> defaults() {
        EnumMap<KleinStarTier, EmcValue> result = new EnumMap<>(KleinStarTier.class);
        for (KleinStarTier tier : KleinStarTier.values()) result.put(tier, tier.defaultCapacity());
        return Map.copyOf(result);
    }
}
