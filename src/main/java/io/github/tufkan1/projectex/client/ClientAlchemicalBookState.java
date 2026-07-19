package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.network.AlchemicalBookAction;
import io.github.tufkan1.projectex.network.AlchemicalBookActionPayload;
import io.github.tufkan1.projectex.network.AlchemicalBookViewPayload;
import java.util.Optional;

/** Ordered client cache; all destinations and costs remain server-authored. */
public final class ClientAlchemicalBookState {
    private AlchemicalBookViewPayload view;
    private long nextRequest;

    public synchronized boolean open(AlchemicalBookViewPayload payload) {
        if (payload.requestId() != -1) return false;
        view = payload;
        nextRequest = 0;
        return true;
    }

    public synchronized boolean accept(AlchemicalBookViewPayload payload) {
        if (view == null || !view.sessionId().equals(payload.sessionId())
            || payload.requestId() < view.requestId()) return false;
        view = payload;
        return true;
    }

    public synchronized Optional<AlchemicalBookActionPayload> action(AlchemicalBookAction action, String name) {
        if (view == null) return Optional.empty();
        return Optional.of(new AlchemicalBookActionPayload(AlchemicalBookActionPayload.PROTOCOL_VERSION,
            view.sessionId(), nextRequest++, action.ordinal(), name));
    }

    public synchronized Optional<AlchemicalBookViewPayload> view() { return Optional.ofNullable(view); }
    public synchronized void close() { view = null; nextRequest = 0; }
}
