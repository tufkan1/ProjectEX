package io.github.tufkan1.projectex.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.automation.AutomationAccess;
import io.github.tufkan1.projectex.automation.AutomationAuthority;
import io.github.tufkan1.projectex.automation.AutomationOperation;
import io.github.tufkan1.projectex.machine.MachineAccess;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecurityPrivilegeMatrixTest {
    @Test
    void automationPrivilegeMatrixNeverGrantsPublicExtractionKnowledgeOrCrafting() {
        UUID owner = new UUID(0, 1);
        UUID member = new UUID(0, 2);
        UUID stranger = new UUID(0, 3);
        AutomationAccess access = new AutomationAccess(owner, new TreeSet<>(java.util.Set.of(member)), true);
        Map<String, AutomationAuthority> actors = Map.of(
            "owner", AutomationAuthority.online(owner, false),
            "member", AutomationAuthority.online(member, false),
            "stranger", AutomationAuthority.online(stranger, false),
            "operator", AutomationAuthority.online(stranger, true),
            "machine", AutomationAuthority.machine());

        for (var actor : actors.entrySet()) {
            for (AutomationOperation operation : AutomationOperation.values()) {
                boolean expected = !actor.getKey().equals("stranger")
                    || operation == AutomationOperation.INSERT_EMC;
                assertEquals(expected, access.permits(actor.getValue(), operation),
                    actor.getKey() + " / " + operation);
            }
        }
    }

    @Test
    void machineOwnershipMatrixRequiresOwnerPublicFlagOrOperator() {
        UUID owner = new UUID(0, 1);
        UUID stranger = new UUID(0, 2);
        MachineAccess privateAccess = MachineAccess.ownedBy(owner);
        assertTrue(privateAccess.permits(owner, false));
        assertFalse(privateAccess.permits(stranger, false));
        assertTrue(privateAccess.permits(stranger, true));
        assertTrue(privateAccess.withPublicAccess(true, owner, false).permits(stranger, false));
        assertTrue(MachineAccess.UNCLAIMED.permits(stranger, false));
    }
}
