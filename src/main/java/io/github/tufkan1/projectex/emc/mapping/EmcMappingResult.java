package io.github.tufkan1.projectex.emc.mapping;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable fixed-point output and diagnostics from one recipe mapping run. */
public record EmcMappingResult(
    Map<EmcKey, EmcValue> values,
    Map<EmcKey, EmcDerivation> derivations,
    Set<EmcKey> unresolvedRecipes,
    int passes
) {
    public EmcMappingResult {
        values = Collections.unmodifiableMap(new TreeMap<>(values));
        derivations = Collections.unmodifiableMap(new TreeMap<>(derivations));
        unresolvedRecipes = Collections.unmodifiableSet(new TreeSet<>(unresolvedRecipes));
    }
}
