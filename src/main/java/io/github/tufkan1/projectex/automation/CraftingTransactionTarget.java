package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Objects;

/**
 * Atomic boundary joining a bound player's EMC mutation to an external item insertion.
 * Implementations must never insert the output unless the account replacement also commits.
 */
public interface CraftingTransactionTarget {
    Snapshot snapshot();

    CommitResult commit(Snapshot expected, PlayerAlchemyState replacement, EmcKey item, int count);

    record Snapshot(PlayerAlchemyState account, long revision) {
        public Snapshot {
            Objects.requireNonNull(account, "account");
            if (revision < 0) {
                throw new IllegalArgumentException("Revision cannot be negative");
            }
        }
    }

    enum CommitResult {
        COMMITTED,
        CONTENTION,
        OUTPUT_REJECTED
    }
}
