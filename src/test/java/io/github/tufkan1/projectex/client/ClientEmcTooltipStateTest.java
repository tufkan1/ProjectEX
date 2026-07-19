package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.network.AlchemyNetworkProtocol;
import io.github.tufkan1.projectex.network.EmcTooltipSyncPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClientEmcTooltipStateTest {
    @Test void acceptsAuthoritativeReplacementAndRejectsStaleRevision() {
        ClientEmcTooltipState state = new ClientEmcTooltipState();
        assertTrue(state.accept(payload(2, "minecraft:diamond", "8192")));
        assertEquals("8192", state.find("minecraft:diamond").orElseThrow());
        assertFalse(state.accept(payload(1, "minecraft:diamond", "1")));
        assertEquals("8192", state.find("minecraft:diamond").orElseThrow());
    }

    @Test void rejectsDuplicateIdsAndClearsOnDisconnect() {
        ClientEmcTooltipState state = new ClientEmcTooltipState();
        assertFalse(state.accept(new EmcTooltipSyncPayload(AlchemyNetworkProtocol.VERSION, 1, List.of(
            new EmcTooltipSyncPayload.Entry("minecraft:coal", "128"),
            new EmcTooltipSyncPayload.Entry("minecraft:coal", "64")))));
        assertTrue(state.accept(payload(1, "minecraft:coal", "128")));
        state.clear();
        assertTrue(state.find("minecraft:coal").isEmpty());
    }

    private static EmcTooltipSyncPayload payload(long revision, String id, String value) {
        return new EmcTooltipSyncPayload(AlchemyNetworkProtocol.VERSION, revision,
            List.of(new EmcTooltipSyncPayload.Entry(id, value)));
    }
}
