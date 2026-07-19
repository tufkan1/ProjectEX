package io.github.tufkan1.projectex.api.storage;

/** Whether a transaction should only be quoted or atomically committed. */
public enum EmcTransferMode {
    SIMULATE,
    EXECUTE
}
