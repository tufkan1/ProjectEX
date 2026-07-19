package io.github.tufkan1.projectex.automation;

@FunctionalInterface
public interface AutomationAuditSink {
    AutomationAuditSink NONE = event -> { };

    void record(AutomationAuditEvent event);
}
