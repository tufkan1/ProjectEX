package example.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcApi;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.fabric.MinecraftEmcAdapter;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Compile-level contract test representing a separate consumer mod. */
class PublicEmcApiCompatibilityTest {
    @Test
    void accessesOnlyTheSupportedPublicSurface() {
        EmcApi api = ProjectEX.emc();
        EmcKey key = MinecraftEmcAdapter.key(
            Identifier.fromNamespaceAndPath("minecraft", "diamond")
        );

        assertNotNull(api.snapshot());
        assertEquals(EmcKey.parse("minecraft:diamond"), key);
        assertEquals(1, EmcApi.VERSION);
    }
}
