package io.github.tufkan1.projectex.emc.reload;

import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable values and winning source IDs produced by one complete reload. */
public record ResolvedEmcData(Map<EmcMatch, EmcValue> values, Map<EmcMatch, String> sources) {
    public ResolvedEmcData {
        values = Collections.unmodifiableMap(new TreeMap<>(values));
        sources = Collections.unmodifiableMap(new TreeMap<>(sources));
        if (!values.keySet().equals(sources.keySet())) {
            throw new IllegalArgumentException("Every resolved EMC value must have exactly one source");
        }
    }
}
