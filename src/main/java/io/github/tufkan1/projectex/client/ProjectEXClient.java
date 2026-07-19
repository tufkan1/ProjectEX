package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.network.AlchemyActionPayload;
import io.github.tufkan1.projectex.network.AlchemyKnowledgePagePayload;
import io.github.tufkan1.projectex.network.AlchemyKnowledgeRequestPayload;
import io.github.tufkan1.projectex.network.AlchemyResultPayload;
import io.github.tufkan1.projectex.network.AlchemySessionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only networking state; screens consume this instead of trusting local calculations. */
@Environment(EnvType.CLIENT)
public final class ProjectEXClient implements ClientModInitializer {
    private static final ClientAlchemySessionState ALCHEMY = new ClientAlchemySessionState();
    private static final ClientKnowledgeBrowserState KNOWLEDGE = new ClientKnowledgeBrowserState();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AlchemySessionPayload.TYPE, (payload, context) -> {
            if (!ALCHEMY.open(payload)) {
                ProjectEX.LOGGER.warn("Discarded malformed ProjectEX alchemy session payload");
            } else {
                KNOWLEDGE.open(payload.sessionId());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AlchemyResultPayload.TYPE, (payload, context) -> {
            if (!ALCHEMY.accept(payload)) {
                ProjectEX.LOGGER.warn("Discarded unexpected ProjectEX alchemy result payload");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(AlchemyKnowledgePagePayload.TYPE, (payload, context) -> {
            if (!KNOWLEDGE.accept(payload)) {
                ProjectEX.LOGGER.warn("Discarded unexpected ProjectEX knowledge page payload");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ALCHEMY.close();
            KNOWLEDGE.close();
        });
    }

    public static boolean sendAction(int operationId, String itemId, int count) {
        try {
            if (!ClientPlayNetworking.canSend(AlchemyActionPayload.TYPE)) {
                return false;
            }
        } catch (IllegalStateException exception) {
            return false;
        }
        return ALCHEMY.nextAction(operationId, itemId, count).map(payload -> {
            try {
                ClientPlayNetworking.send(payload);
                return true;
            } catch (IllegalStateException exception) {
                ProjectEX.LOGGER.warn("Could not send ProjectEX alchemy action", exception);
                return false;
            }
        }).orElse(false);
    }

    public static ClientAlchemySessionState alchemy() {
        return ALCHEMY;
    }

    public static boolean requestKnowledge(String query, int page, int pageSize) {
        try {
            if (!ClientPlayNetworking.canSend(AlchemyKnowledgeRequestPayload.TYPE)) {
                return false;
            }
        } catch (IllegalStateException exception) {
            return false;
        }
        return KNOWLEDGE.nextQuery(query, page, pageSize).map(payload -> {
            try {
                ClientPlayNetworking.send(payload);
                return true;
            } catch (IllegalStateException exception) {
                ProjectEX.LOGGER.warn("Could not send ProjectEX knowledge query", exception);
                return false;
            }
        }).orElse(false);
    }

    public static ClientKnowledgeBrowserState knowledge() {
        return KNOWLEDGE;
    }
}
