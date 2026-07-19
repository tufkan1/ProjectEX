package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** A bounded server-side availability query; callers provide candidates instead of receiving the full index. */
public record KnowledgeQuery(UUID requestId, long tick, SortedSet<EmcKey> candidates) {
    public KnowledgeQuery {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(candidates, "candidates");
        if (tick < 0) {
            throw new IllegalArgumentException("Tick cannot be negative");
        }
        candidates = Collections.unmodifiableSortedSet(new TreeSet<>(candidates));
    }
}
