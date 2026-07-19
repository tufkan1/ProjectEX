package io.github.tufkan1.projectex.api.alchemy;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Claim mods may veto ProjectEX world changes without accepting client authority. */
@FunctionalInterface
public interface WorldTransmutationProtection {
    Event<WorldTransmutationProtection> EVENT = EventFactory.createArrayBacked(
        WorldTransmutationProtection.class,
        listeners -> context -> {
            for (WorldTransmutationProtection listener : listeners) {
                if (!listener.canTransform(context)) {
                    return false;
                }
            }
            return true;
        }
    );

    /** Returns false to reject the complete planned transformation. */
    boolean canTransform(WorldTransmutationContext context);
}
