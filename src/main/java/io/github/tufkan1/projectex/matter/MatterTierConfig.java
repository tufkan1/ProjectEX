package io.github.tufkan1.projectex.matter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Atomically published, immutable matter-tier snapshot used by server runtime actions. */
public final class MatterTierConfig {
    private static final AtomicReference<Map<String, MatterTier>> SNAPSHOT =
        new AtomicReference<>(MatterTier.DEFAULTS);

    private MatterTierConfig() { }

    public static MatterTier resolve(MatterTier fallback) {
        return SNAPSHOT.get().getOrDefault(fallback.id(), fallback);
    }

    public static Map<String, MatterTier> snapshot() { return SNAPSHOT.get(); }

    public static void publish(Map<String, MatterTier> tiers) {
        Map<String, MatterTier> copy = Map.copyOf(tiers);
        if (!copy.keySet().equals(MatterTier.DEFAULTS.keySet())) {
            throw new IllegalArgumentException("Matter tier snapshot must define dark_matter and red_matter");
        }
        copy.forEach((id, tier) -> {
            if (!id.equals(tier.id()) || tier.furnaceOutputSlots() > 18) {
                throw new IllegalArgumentException("Unsafe runtime matter tier: " + id);
            }
        });
        SNAPSHOT.set(copy);
    }
}
