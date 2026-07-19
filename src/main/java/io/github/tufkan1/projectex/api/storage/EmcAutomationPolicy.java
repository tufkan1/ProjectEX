package io.github.tufkan1.projectex.api.storage;

/** Machine access policy declared by an EMC storage implementation. */
public enum EmcAutomationPolicy {
    NONE(false, false),
    INSERT_ONLY(true, false),
    EXTRACT_ONLY(false, true),
    INPUT_OUTPUT(true, true);

    private final boolean insert;
    private final boolean extract;

    EmcAutomationPolicy(boolean insert, boolean extract) {
        this.insert = insert;
        this.extract = extract;
    }

    public boolean allows(EmcStorageOperation operation) {
        return operation == EmcStorageOperation.INSERT ? insert : extract;
    }
}
