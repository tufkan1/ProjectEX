package io.github.tufkan1.projectex.storage;

import java.util.Objects;
import java.util.Set;

/** Rejects recursive portable inventories before any item transaction commits. */
public final class BagNestingPolicy {
    private final Set<String> portableContainerItemIds;

    public BagNestingPolicy(Set<String> portableContainerItemIds) {
        this.portableContainerItemIds = Set.copyOf(portableContainerItemIds);
    }

    public boolean canInsert(BagIdentity destination, String candidateItemId, BagIdentity candidateBag) {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(candidateItemId, "candidateItemId");
        if (candidateBag != null && candidateBag.bagId().equals(destination.bagId())) {
            return false;
        }
        return candidateBag == null && !portableContainerItemIds.contains(candidateItemId);
    }
}
