package io.github.tufkan1.projectex.machine;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable ownership policy used by every server-owned EMC machine. */
public record MachineAccess(Optional<UUID> owner, boolean publicAccess) {
    public static final MachineAccess UNCLAIMED = new MachineAccess(Optional.empty(), false);

    public MachineAccess {
        Objects.requireNonNull(owner, "owner");
    }

    public static MachineAccess ownedBy(UUID owner) {
        return new MachineAccess(Optional.of(Objects.requireNonNull(owner, "owner")), false);
    }

    public boolean permits(UUID actor, boolean operatorOverride) {
        return operatorOverride || publicAccess || owner.isEmpty() || owner.get().equals(actor);
    }

    public MachineAccess claim(UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return owner.isEmpty() ? ownedBy(actor) : this;
    }

    public MachineAccess withPublicAccess(boolean enabled, UUID actor, boolean operatorOverride) {
        if (!permits(actor, operatorOverride)) {
            throw new SecurityException("Actor cannot change machine access");
        }
        return new MachineAccess(owner, enabled);
    }
}
