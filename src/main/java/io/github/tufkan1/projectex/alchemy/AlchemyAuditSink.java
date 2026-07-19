package io.github.tufkan1.projectex.alchemy;

/** Receives one event for every accepted or rejected transaction attempt. */
@FunctionalInterface
public interface AlchemyAuditSink {
    AlchemyAuditSink NOOP = event -> {
    };

    void record(AlchemyAuditEvent event);
}
