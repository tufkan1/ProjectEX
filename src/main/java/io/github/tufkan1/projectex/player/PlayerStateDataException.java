package io.github.tufkan1.projectex.player;

/** Indicates malformed, unsupported, or unsafe persistent player alchemy data. */
public final class PlayerStateDataException extends RuntimeException {
    public PlayerStateDataException(String message) {
        super(message);
    }

    public PlayerStateDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
