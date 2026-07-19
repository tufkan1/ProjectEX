package io.github.tufkan1.projectex.storage;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Versioned color-keyed portable inventory identity and ownership record. */
public record BagIdentity(int version, UUID bagId, String color, Optional<UUID> owner) {
    public static final int CURRENT_VERSION = 1;

    public BagIdentity {
        Objects.requireNonNull(bagId, "bagId");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(owner, "owner");
        if (version != CURRENT_VERSION || color.isBlank()) {
            throw new IllegalArgumentException("Invalid alchemical bag identity");
        }
    }

    public static BagIdentity create(String color, UUID owner) {
        return new BagIdentity(CURRENT_VERSION, UUID.randomUUID(), color, Optional.of(owner));
    }

    public boolean permits(UUID actor, boolean operatorOverride) {
        return operatorOverride || owner.isEmpty() || owner.get().equals(actor);
    }
}
