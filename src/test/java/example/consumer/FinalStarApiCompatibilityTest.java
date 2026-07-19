package example.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.tufkan1.projectex.api.endgame.FinalStarApi;
import io.github.tufkan1.projectex.api.endgame.FinalStarCapability;
import io.github.tufkan1.projectex.api.endgame.FinalStarContext;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import org.junit.jupiter.api.Test;

/** Compile-time example of a third-party mod consuming the Final Star contract. */
class FinalStarApiCompatibilityTest {
    @Test void externalModCanReferenceStableLookupContract() {
        ItemApiLookup<FinalStarCapability, FinalStarContext> lookup = FinalStarApi.LOOKUP;
        assertNotNull(lookup);
        assertEquals(1, FinalStarApi.VERSION);
        assertEquals(1, FinalStarCapability.VERSION);
    }
}
