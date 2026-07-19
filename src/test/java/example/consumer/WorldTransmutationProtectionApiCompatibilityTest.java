package example.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection;
import org.junit.jupiter.api.Test;

/** Compile-time example of a claim integration consuming the public protection hook. */
class WorldTransmutationProtectionApiCompatibilityTest {
    @Test
    void externalModCanImplementProtectionCallback() {
        WorldTransmutationProtection callback = context -> context.player() != null;

        assertNotNull(callback);
    }
}
