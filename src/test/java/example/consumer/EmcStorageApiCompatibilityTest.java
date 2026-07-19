package example.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.api.storage.EmcStorage;
import io.github.tufkan1.projectex.api.storage.EmcStorageApi;
import io.github.tufkan1.projectex.api.storage.EmcStorageContext;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import org.junit.jupiter.api.Test;

/** Compile-time example of a third-party mod consuming the capability-style API. */
class EmcStorageApiCompatibilityTest {
    @Test
    void externalModCanReferenceStableLookupContract() {
        ItemApiLookup<EmcStorage, EmcStorageContext> lookup = EmcStorageApi.LOOKUP;

        assertNotNull(lookup);
        assertEquals(1, EmcStorageApi.VERSION);
    }
}
