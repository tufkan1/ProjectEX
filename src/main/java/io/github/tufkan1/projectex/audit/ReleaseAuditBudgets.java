package io.github.tufkan1.projectex.audit;

/** Frozen 1.0 regression ceilings exercised by release-audit tests and CI. */
public final class ReleaseAuditBudgets {
    public static final int SUPPORTED_PLAYERS = 128;
    public static final int SUPPORTED_MACHINES_PER_LEVEL = 1_024;
    public static final int MACHINE_SOAK_TICKS = 200;
    public static final long MAX_AUDIT_CPU_MILLIS = 5_000;
    public static final long MAX_AUDIT_ALLOCATED_BYTES = 512L * 1_024 * 1_024;
    public static final int MAX_ACTION_PACKET_BYTES = 512;
    public static final int MAX_KNOWLEDGE_PAGE_PACKET_BYTES = 72 * 1_024;
    public static final int MAX_ALCHEMICAL_BOOK_PACKET_BYTES = 32 * 1_024;
    public static final int MAX_PLAYER_SAVE_BYTES = 2 * 1_024 * 1_024;
    public static final long MAX_RUNTIME_JAR_BYTES = 4L * 1_024 * 1_024;
    private ReleaseAuditBudgets() { }
}
