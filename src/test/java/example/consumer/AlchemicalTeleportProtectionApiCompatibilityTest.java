package example.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.api.teleport.AlchemicalTeleportProtection;
import org.junit.jupiter.api.Test;

class AlchemicalTeleportProtectionApiCompatibilityTest {
    @Test void externalModCanImplementTeleportProtectionCallback() {
        AlchemicalTeleportProtection callback = context -> context.player() != null;
        assertNotNull(callback);
    }
}
