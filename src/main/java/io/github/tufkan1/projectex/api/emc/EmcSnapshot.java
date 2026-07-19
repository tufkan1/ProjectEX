package io.github.tufkan1.projectex.api.emc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** One immutable, internally consistent generation of the EMC registry. */
public record EmcSnapshot(
    long revision,
    Map<EmcMatch, EmcValue> values,
    Map<EmcMatch, String> sources
) {
    public EmcSnapshot {
        values = Collections.unmodifiableMap(new TreeMap<>(values));
        sources = Collections.unmodifiableMap(new TreeMap<>(sources));
        if (!values.keySet().equals(sources.keySet())) {
            throw new IllegalArgumentException("EMC values and sources must contain identical keys");
        }
    }

    public Optional<EmcValue> find(EmcKey key) {
        return find(EmcMatch.item(key));
    }

    public Optional<EmcValue> find(EmcMatch match) {
        return Optional.ofNullable(values.get(match));
    }

    public Optional<String> findSource(EmcMatch match) {
        return Optional.ofNullable(sources.get(match));
    }

    public int size() {
        return values.size();
    }
}
