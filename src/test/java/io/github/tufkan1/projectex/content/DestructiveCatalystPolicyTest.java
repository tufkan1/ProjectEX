package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DestructiveCatalystPolicyTest {
    @Test void acceptsOnlyExplicitBooleans() {
        assertTrue(DestructiveCatalystPolicy.parse("true"));
        assertFalse(DestructiveCatalystPolicy.parse("FALSE"));
        assertThrows(IllegalArgumentException.class,
            () -> DestructiveCatalystPolicy.parse("yes"));
    }
}
