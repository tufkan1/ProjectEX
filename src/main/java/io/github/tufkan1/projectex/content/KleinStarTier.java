package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcAutomationPolicy;

/** Baseline Klein Star progression and exact storage capacities. */
public enum KleinStarTier {
    EIN("ein", 50_000L),
    ZWEI("zwei", 200_000L),
    DREI("drei", 800_000L),
    VIER("vier", 3_200_000L),
    SPHERE("sphere", 12_800_000L),
    OMEGA("omega", 51_200_000L);

    private final String serializedName;
    private final EmcValue capacity;

    KleinStarTier(String serializedName, long capacity) {
        this.serializedName = serializedName;
        this.capacity = EmcValue.of(capacity);
    }

    public String serializedName() {
        return serializedName;
    }

    public EmcValue capacity() {
        return capacity;
    }

    public EmcAutomationPolicy automationPolicy() {
        return EmcAutomationPolicy.INPUT_OUTPUT;
    }

    public KleinStarTier next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }
}
