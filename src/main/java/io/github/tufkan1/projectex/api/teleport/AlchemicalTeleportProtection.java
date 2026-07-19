package io.github.tufkan1.projectex.api.teleport;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Claim and safety integrations may veto an Alchemical Book teleport before payment. */
@FunctionalInterface
public interface AlchemicalTeleportProtection {
    Event<AlchemicalTeleportProtection> EVENT = EventFactory.createArrayBacked(
        AlchemicalTeleportProtection.class,
        listeners -> context -> {
            for (AlchemicalTeleportProtection listener : listeners) {
                if (!listener.canTeleport(context)) return false;
            }
            return true;
        }
    );

    boolean canTeleport(AlchemicalTeleportContext context);
}
