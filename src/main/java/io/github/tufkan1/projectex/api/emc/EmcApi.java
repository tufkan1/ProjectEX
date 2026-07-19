package io.github.tufkan1.projectex.api.emc;

import java.util.Optional;

/**
 * Query-only ProjectEX EMC API.
 *
 * <p>All reads observe an immutable, atomically published snapshot. Reload callbacks run after
 * publication on the server reload thread; consumers must not block that thread.</p>
 */
public interface EmcApi {
    /** API contract version. It changes only when this interface makes a breaking change. */
    int VERSION = 1;

    /** Finds the componentless value for an item identifier. */
    Optional<EmcValue> find(EmcKey key);

    /** Finds an exact item/component match. */
    Optional<EmcValue> find(EmcMatch match);

    /** Returns the current immutable view. */
    EmcSnapshot snapshot();

    /**
     * Registers a post-publication reload listener.
     *
     * @return a subscription whose {@link EmcSubscription#close()} method unregisters the listener
     */
    EmcSubscription subscribe(EmcReloadListener listener);
}
