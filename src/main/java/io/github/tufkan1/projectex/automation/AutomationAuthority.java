package io.github.tufkan1.projectex.automation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-verified online actor or the persisted machine binding itself. */
public record AutomationAuthority(Optional<UUID> actor, boolean operator, boolean machineBinding) {
    public AutomationAuthority {
        Objects.requireNonNull(actor, "actor");
        if (operator && actor.isEmpty()) {
            throw new IllegalArgumentException("Operator authority requires an online actor");
        }
        if (machineBinding && actor.isPresent()) {
            throw new IllegalArgumentException("Machine binding cannot impersonate a player");
        }
    }

    public static AutomationAuthority online(UUID actor, boolean operator) {
        return new AutomationAuthority(Optional.of(Objects.requireNonNull(actor, "actor")), operator, false);
    }

    public static AutomationAuthority machine() {
        return new AutomationAuthority(Optional.empty(), false, true);
    }
}
