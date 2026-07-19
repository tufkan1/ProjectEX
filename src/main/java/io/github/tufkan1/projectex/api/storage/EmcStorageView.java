package io.github.tufkan1.projectex.api.storage;

import io.github.tufkan1.projectex.api.emc.EmcValue;

/** Query-only snapshot exposed safely to UI and integrations. */
public interface EmcStorageView {
    EmcValue stored();

    EmcValue capacity();

    boolean allows(EmcStorageOperation operation);
}
