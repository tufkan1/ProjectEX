package io.github.tufkan1.projectex.api.utility;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Claim and protection integrations may veto each bounded utility target. */
@FunctionalInterface
public interface UtilityWorldActionProtection {
    Event<UtilityWorldActionProtection> EVENT = EventFactory.createArrayBacked(
        UtilityWorldActionProtection.class,
        listeners -> context -> {
            for (UtilityWorldActionProtection listener : listeners) {
                if (!listener.canAct(context)) return false;
            }
            return true;
        }
    );

    boolean canAct(UtilityWorldActionContext context);
}
