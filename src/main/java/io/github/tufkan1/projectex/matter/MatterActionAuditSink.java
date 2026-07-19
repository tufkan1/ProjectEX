package io.github.tufkan1.projectex.matter;

@FunctionalInterface
public interface MatterActionAuditSink {
    MatterActionAuditSink NOOP = event -> { };
    void record(MatterActionAuditEvent event);
}
