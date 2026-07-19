package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.util.Objects;
import java.util.UUID;

/** One server-priced, uniquely identified crafting extraction request. */
public record TransmutationCraftRequest(
    UUID requestId,
    long tick,
    EmcKey item,
    long emcRevision,
    int count
) {
    public TransmutationCraftRequest {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(item, "item");
        if (tick < 0 || emcRevision < 0 || count < 1 || count > 64) {
            throw new IllegalArgumentException("Invalid transmutation crafting request");
        }
    }
}
