package io.github.tufkan1.projectex.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class AlchemicalBookConfigTest {
    @AfterEach void clear() { System.clearProperty(AlchemicalBookConfig.EDIT_POLICY); AlchemicalBookConfig.reload(); }
    @Test void parsesOperatorPolicyAndRejectsUnknownValues() {
        System.setProperty(AlchemicalBookConfig.EDIT_POLICY, "operator_only");
        assertEquals(AlchemicalBookConfig.EditPolicy.OPERATOR_ONLY, AlchemicalBookConfig.load());
        System.setProperty(AlchemicalBookConfig.EDIT_POLICY, "friends");
        assertThrows(IllegalArgumentException.class, AlchemicalBookConfig::load);
    }
}
