package io.github.tufkan1.projectex.emc.reload;

import java.util.concurrent.atomic.AtomicLong;

/** Last complete EMC data-pack reload attempt, retained for operator diagnostics. */
public final class EmcReloadDiagnostics {
    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static volatile Snapshot snapshot = new Snapshot(0, true, 0, 0, 0, "");
    private EmcReloadDiagnostics() { }

    public static Snapshot snapshot() { return snapshot; }

    static void success(int resources, int candidates, int values) {
        snapshot = new Snapshot(ATTEMPTS.incrementAndGet(), true, resources, candidates, values, "");
    }

    static void failure(int resources, int candidates, RuntimeException failure) {
        String message = failure.getMessage() == null ? failure.getClass().getName() : failure.getMessage();
        if (message.length() > 1024) message = message.substring(0, 1024);
        snapshot = new Snapshot(ATTEMPTS.incrementAndGet(), false, resources, candidates, 0, message);
    }

    public record Snapshot(
        long attempt, boolean successful, int resourceCount, int candidateCount, int valueCount, String failure
    ) { }
}
