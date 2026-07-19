package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** One uniquely identified, tick-bound EMC Link transaction. */
public record EmcLinkRequest(
    UUID requestId,
    long tick,
    AutomationOperation operation,
    EmcValue amount,
    Optional<EmcKey> item
) {
    public EmcLinkRequest {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(item, "item");
        if (tick < 0 || amount.equals(EmcValue.ZERO)
            || (operation != AutomationOperation.INSERT_EMC
                && operation != AutomationOperation.EXTRACT_EMC)) {
            throw new IllegalArgumentException("Invalid EMC Link request");
        }
    }
}
