package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.network.AlchemicalBookAction;
import io.github.tufkan1.projectex.network.AlchemicalBookViewPayload;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ClientAlchemicalBookStateTest {
    @Test void ordersActionsAndRejectsForeignOrOlderViews() {
        ClientAlchemicalBookState state = new ClientAlchemicalBookState();
        UUID session = UUID.randomUUID();
        assertTrue(state.open(view(session, -1)));
        assertEquals(0, state.action(AlchemicalBookAction.CREATE, "Home").orElseThrow().requestId());
        assertEquals(1, state.action(AlchemicalBookAction.BACK, "").orElseThrow().requestId());
        assertTrue(state.accept(view(session, 1)));
        assertFalse(state.accept(view(session, 0)));
        assertFalse(state.accept(view(UUID.randomUUID(), 2)));
    }

    private static AlchemicalBookViewPayload view(UUID session, long request) {
        return new AlchemicalBookViewPayload(session, request, 0, true, "0", "",
            List.of(), Optional.empty());
    }
}
