package io.github.tufkan1.projectex.emc.reload;

import io.github.tufkan1.projectex.emc.data.EmcDataFile;
import java.util.Objects;

/** A parsed EMC resource together with its stable pack/resource provenance. */
public record EmcDataSource(String sourceId, EmcDataFile data) {
    public EmcDataSource {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(data, "data");
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId cannot be blank");
        }
    }
}
