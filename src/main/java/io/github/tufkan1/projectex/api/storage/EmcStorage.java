package io.github.tufkan1.projectex.api.storage;

import io.github.tufkan1.projectex.api.emc.EmcValue;

/** Server-bound capability for atomic insert and extract transactions. */
public interface EmcStorage extends EmcStorageView {
    EmcTransferResult insert(EmcValue requested, EmcTransferMode mode);

    EmcTransferResult extract(EmcValue requested, EmcTransferMode mode);
}
