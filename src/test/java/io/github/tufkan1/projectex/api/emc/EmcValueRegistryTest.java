package io.github.tufkan1.projectex.api.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmcValueRegistryTest {
    @Test
    void replacesTheSnapshotAtomicallyAndExposesItAsImmutable() {
        EmcValueRegistry registry = new EmcValueRegistry();
        EmcKey coal = EmcKey.parse("minecraft:coal");
        EmcMatch coalMatch = EmcMatch.item(coal);
        registry.replaceAll(Map.of(coalMatch, EmcValue.of(128)), Map.of(coalMatch, "base"));

        assertEquals(EmcValue.of(128), registry.find(coal).orElseThrow());
        assertEquals(1, registry.snapshot().size());
        assertThrows(UnsupportedOperationException.class,
            () -> registry.snapshot().values().put(
                EmcMatch.item(EmcKey.parse("minecraft:diamond")), EmcValue.of(8192)));
    }

    @Test
    void rejectsMismatchedProvenanceWithoutChangingTheLiveSnapshot() {
        EmcValueRegistry registry = new EmcValueRegistry();
        EmcMatch coal = EmcMatch.item(EmcKey.parse("minecraft:coal"));
        registry.replaceAll(Map.of(coal, EmcValue.of(128)), Map.of(coal, "base"));

        assertThrows(IllegalArgumentException.class,
            () -> registry.replaceAll(Map.of(coal, EmcValue.of(64)), Map.of()));
        assertEquals(EmcValue.of(128), registry.find(coal).orElseThrow());
        assertEquals("base", registry.snapshot().findSource(coal).orElseThrow());
    }

    @Test
    void publishesMonotonicSnapshotsAndSupportsUnsubscription() {
        EmcValueRegistry registry = new EmcValueRegistry();
        List<Long> revisions = new ArrayList<>();
        EmcSubscription subscription = registry.subscribe(snapshot -> revisions.add(snapshot.revision()));
        EmcMatch coal = EmcMatch.item(EmcKey.parse("minecraft:coal"));

        registry.replaceAll(Map.of(coal, EmcValue.of(128)), Map.of(coal, "base"));
        subscription.close();
        registry.replaceAll(Map.of(coal, EmcValue.of(64)), Map.of(coal, "override"));

        assertEquals(List.of(1L), revisions);
        assertEquals(2L, registry.snapshot().revision());
    }

    @Test
    void stagedReloadDataIsInvisibleUntilTheFinalRecipePublication() {
        EmcValueRegistry registry = new EmcValueRegistry();
        EmcMatch coal = EmcMatch.item(EmcKey.parse("minecraft:coal"));

        registry.stageAll(Map.of(coal, EmcValue.of(128)), Map.of(coal, "base"));

        assertEquals(0, registry.snapshot().size());
        assertEquals(EmcValue.of(128), registry.stagedSnapshot().find(coal).orElseThrow());
        registry.replaceAll(registry.stagedSnapshot().values(), registry.stagedSnapshot().sources());
        assertEquals(EmcValue.of(128), registry.snapshot().find(coal).orElseThrow());
    }
}
