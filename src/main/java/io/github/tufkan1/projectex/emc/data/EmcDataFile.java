package io.github.tufkan1.projectex.emc.data;

import java.util.List;

/** Fully validated contents of one EMC data-pack resource. */
public record EmcDataFile(int schemaVersion, int priority, List<EmcDefinition> values) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MIN_PRIORITY = -10_000;
    public static final int MAX_PRIORITY = 10_000;

    public EmcDataFile {
        values = List.copyOf(values);
    }
}
