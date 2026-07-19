package io.github.tufkan1.projectex.emc.mapping;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Explainable winning recipe calculation for one derived item. */
public record EmcDerivation(
    EmcValue value,
    EmcKey recipe,
    Map<EmcKey, Integer> chosenInputs,
    Map<EmcKey, Integer> returnedRemainders
) {
    public EmcDerivation {
        chosenInputs = Collections.unmodifiableMap(new TreeMap<>(chosenInputs));
        returnedRemainders = Collections.unmodifiableMap(new TreeMap<>(returnedRemainders));
    }
}
