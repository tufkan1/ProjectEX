package io.github.tufkan1.projectex.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmcReloadDiagnosticsTest {
    @Test
    void retainsBoundedSuccessAndFailureDetailsForOperators() {
        EmcReloadDiagnostics.success(2, 3, 4);
        var success = EmcReloadDiagnostics.snapshot();
        assertTrue(success.successful());
        assertEquals(4, success.valueCount());

        EmcReloadDiagnostics.failure(5, 6, new IllegalArgumentException("conflict".repeat(300)));
        var failure = EmcReloadDiagnostics.snapshot();
        assertFalse(failure.successful());
        assertEquals(5, failure.resourceCount());
        assertEquals(1024, failure.failure().length());
    }
}
