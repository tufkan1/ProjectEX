package io.github.tufkan1.projectex.api.matter;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Claim mods may veto each position in a bounded matter-tool plan. */
@FunctionalInterface
public interface MatterAreaActionProtection {
    Event<MatterAreaActionProtection> EVENT = EventFactory.createArrayBacked(
        MatterAreaActionProtection.class,
        listeners -> context -> {
            for (MatterAreaActionProtection listener : listeners) {
                if (!listener.canAct(context)) return false;
            }
            return true;
        }
    );

    boolean canAct(MatterAreaActionContext context);
}
