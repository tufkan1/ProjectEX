package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KnowledgeTomePolicyTest {
    @Test void parsesEveryExplicitPolicyCaseInsensitively() {
        assertEquals(KnowledgeTomePolicy.Mode.DISABLED,
            KnowledgeTomePolicy.parse("disabled"));
        assertEquals(KnowledgeTomePolicy.Mode.CONSUME,
            KnowledgeTomePolicy.parse(" CONSUME "));
        assertEquals(KnowledgeTomePolicy.Mode.OPERATOR_ONLY,
            KnowledgeTomePolicy.parse("operator_only"));
    }

    @Test void rejectsUnknownOrBlankPolicyWithoutFallback() {
        assertThrows(IllegalArgumentException.class,
            () -> KnowledgeTomePolicy.parse("enabled"));
        assertThrows(IllegalArgumentException.class,
            () -> KnowledgeTomePolicy.parse(""));
    }
}
