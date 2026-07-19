package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.network.AlchemyKnowledgePagePayload;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientKnowledgeBrowserStateTest {
    @Test
    void createsBoundedMonotonicQueriesAndAcceptsOnlyExpectedPages() {
        ClientKnowledgeBrowserState state = new ClientKnowledgeBrowserState();
        state.open(10);

        var first = state.nextQuery("coal", 0, 18).orElseThrow();
        var second = state.nextQuery("diamond", 0, 18).orElseThrow();
        assertEquals(0, first.queryId());
        assertEquals(1, second.queryId());
        assertTrue(state.nextQuery("", -1, 18).isEmpty());

        AlchemyKnowledgePagePayload newest = page(10, 1, "minecraft:diamond");
        assertTrue(state.accept(newest));
        assertFalse(state.accept(page(10, 0, "minecraft:coal")));
        assertFalse(state.accept(page(11, 1, "minecraft:coal")));
        assertEquals("minecraft:diamond", state.snapshot().entries().getFirst().itemId());
    }

    @Test
    void favoritesAreDeterministicAndSurviveSessionReconnectInMemory() {
        ClientKnowledgeBrowserState state = new ClientKnowledgeBrowserState();
        state.open(10);
        state.nextQuery("", 0, 18).orElseThrow();
        state.accept(page(10, 0, "minecraft:coal"));

        assertTrue(state.toggleFavorite("minecraft:coal"));
        assertEquals(1, state.snapshot().visibleFavoriteCount());
        assertFalse(state.toggleFavorite("not an id"));
        state.close();
        state.open(20);
        assertTrue(state.snapshot().favorites().contains("minecraft:coal"));
    }

    @Test
    void typedFailureClearsResultsAndIsExposedForNarration() {
        ClientKnowledgeBrowserState state = new ClientKnowledgeBrowserState();
        state.open(10);
        state.nextQuery("", 0, 18).orElseThrow();
        AlchemyKnowledgePagePayload failure = new AlchemyKnowledgePagePayload(
            1, 10, 0, AlchemyTransactionFailure.RATE_LIMITED.ordinal(),
            0, 0, 0, List.of());

        assertTrue(state.accept(failure));
        assertTrue(state.snapshot().entries().isEmpty());
        assertEquals(AlchemyTransactionFailure.RATE_LIMITED,
            state.snapshot().lastFailure().orElseThrow());
    }

    @Test
    void replacesFavoritesWithOnlyValidBoundedIdentifiers() {
        ClientKnowledgeBrowserState state = new ClientKnowledgeBrowserState();
        state.replaceFavorites(List.of("minecraft:diamond", "invalid", "minecraft:coal"));

        assertEquals(new java.util.TreeSet<>(List.of("minecraft:coal", "minecraft:diamond")),
            state.snapshot().favorites());
    }

    private static AlchemyKnowledgePagePayload page(long session, long query, String item) {
        return new AlchemyKnowledgePagePayload(
            1,
            session,
            query,
            AlchemyTransactionFailure.NONE.ordinal(),
            0,
            1,
            1,
            List.of(new AlchemyKnowledgePagePayload.Entry(item, "128"))
        );
    }
}
