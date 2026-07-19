package io.github.tufkan1.projectex.emc.data;

/** Indicates that an EMC data file is malformed or violates a safety invariant. */
public final class EmcDataException extends RuntimeException {
    public EmcDataException(String message) {
        super(message);
    }

    public EmcDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
