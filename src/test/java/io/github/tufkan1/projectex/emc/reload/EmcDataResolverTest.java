package io.github.tufkan1.projectex.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.emc.data.EmcDataException;
import io.github.tufkan1.projectex.emc.data.EmcDataFile;
import io.github.tufkan1.projectex.emc.data.EmcDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmcDataResolverTest {
    private static final EmcKey COAL = EmcKey.parse("minecraft:coal");
    private static final EmcKey CHARCOAL = EmcKey.parse("minecraft:charcoal");

    @Test
    void higherPriorityWinsRegardlessOfInputOrder() {
        EmcDataSource low = source("z-low", 0, value(COAL, 64));
        EmcDataSource high = source("a-high", 10, value(COAL, 128));

        ResolvedEmcData forward = EmcDataResolver.resolve(List.of(low, high));
        ResolvedEmcData reverse = EmcDataResolver.resolve(List.of(high, low));

        assertEquals(EmcValue.of(128), forward.values().get(EmcMatch.item(COAL)));
        assertEquals(forward, reverse);
        assertEquals("a-high", forward.sources().get(EmcMatch.item(COAL)));
    }

    @Test
    void equalPriorityConflictsAreFatal() {
        EmcDataSource first = source("first", 0, value(COAL, 64));
        EmcDataSource second = source("second", 0, value(COAL, 128));

        assertThrows(EmcDataException.class, () -> EmcDataResolver.resolve(List.of(first, second)));
    }

    @Test
    void identicalDefinitionsCoalesceDeterministically() {
        ResolvedEmcData resolved = EmcDataResolver.resolve(List.of(
            source("z-pack", 0, value(COAL, 128)),
            source("a-pack", 0, value(COAL, 128))
        ));

        assertEquals("a-pack", resolved.sources().get(EmcMatch.item(COAL)));
    }

    @Test
    void resolvesAliasChains() {
        EmcKey synthetic = EmcKey.parse("example:synthetic_coal");
        ResolvedEmcData resolved = EmcDataResolver.resolve(List.of(source("pack", 0,
            value(COAL, 128), alias(CHARCOAL, COAL), alias(synthetic, CHARCOAL))));

        assertEquals(EmcValue.of(128), resolved.values().get(EmcMatch.item(synthetic)));
    }

    @Test
    void rejectsAliasCyclesAndMissingTargets() {
        EmcKey first = EmcKey.parse("example:first");
        EmcKey second = EmcKey.parse("example:second");

        assertThrows(EmcDataException.class, () -> EmcDataResolver.resolve(List.of(
            source("cycle", 0, alias(first, second), alias(second, first)))));
        assertThrows(EmcDataException.class, () -> EmcDataResolver.resolve(List.of(
            source("missing", 0, alias(first, second)))));
    }

    @Test
    void removalsSuppressValuesAndCannotBeAliasTargets() {
        EmcKey removed = EmcKey.parse("example:removed");
        ResolvedEmcData resolved = EmcDataResolver.resolve(List.of(
            source("base", 0, value(removed, 10)),
            source("override", 1, remove(removed))
        ));
        assertFalse(resolved.values().containsKey(EmcMatch.item(removed)));

        assertThrows(EmcDataException.class, () -> EmcDataResolver.resolve(List.of(
            source("pack", 0, remove(removed), alias(COAL, removed)))));
    }

    @Test
    void componentSpecificValuesRemainDistinct() {
        EmcDefinition charged = new EmcDefinition(COAL, "{\"example:charge\":100}",
            EmcDefinition.Kind.VALUE, EmcValue.of(1024), null);
        ResolvedEmcData resolved = EmcDataResolver.resolve(List.of(
            source("pack", 0, value(COAL, 128), charged)));

        assertEquals(2, resolved.values().size());
        assertEquals(EmcValue.of(1024), resolved.values().get(charged.match()));
    }

    private static EmcDataSource source(String id, int priority, EmcDefinition... definitions) {
        return new EmcDataSource(id, new EmcDataFile(1, priority, List.of(definitions)));
    }

    private static EmcDefinition value(EmcKey item, long amount) {
        return new EmcDefinition(item, null, EmcDefinition.Kind.VALUE, EmcValue.of(amount), null);
    }

    private static EmcDefinition alias(EmcKey item, EmcKey target) {
        return new EmcDefinition(item, null, EmcDefinition.Kind.ALIAS, null, target);
    }

    private static EmcDefinition remove(EmcKey item) {
        return new EmcDefinition(item, null, EmcDefinition.Kind.REMOVE, null, null);
    }
}
