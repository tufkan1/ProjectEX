package io.github.tufkan1.projectex.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class UtilityStatePayloadTest {
    @Test
    void acceptsOnlyKnownActionsAndHands() {
        UtilityStatePayload chargeMain = new UtilityStatePayload(
            UtilityStateAction.CHARGE.ordinal(), 0
        );
        assertTrue(chargeMain.hasValidShape());
        assertEquals(UtilityStateAction.CHARGE, chargeMain.resolvedAction());
        assertTrue(new UtilityStatePayload(UtilityStateAction.MODE.ordinal(), 1).hasValidShape());
        assertFalse(new UtilityStatePayload(-1, 0).hasValidShape());
        assertFalse(new UtilityStatePayload(2, 0).hasValidShape());
        assertFalse(new UtilityStatePayload(0, -1).hasValidShape());
        assertFalse(new UtilityStatePayload(0, 2).hasValidShape());
    }
}
