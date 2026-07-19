package example.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.api.matter.MatterAreaActionProtection;
import org.junit.jupiter.api.Test;

/** Compiles only against the public protection hook exposed to claim mods. */
final class MatterAreaActionProtectionApiCompatibilityTest {
    @Test
    void publicProtectionEventIsAvailable() {
        MatterAreaActionProtection callback = context -> context.action().equals("mine");
        assertNotNull(callback);
        assertNotNull(MatterAreaActionProtection.EVENT);
    }
}
