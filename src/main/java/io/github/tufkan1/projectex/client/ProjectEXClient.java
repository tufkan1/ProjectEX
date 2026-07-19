package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.client.screen.TransmutationScreen;
import io.github.tufkan1.projectex.client.screen.EmcMachineScreen;
import io.github.tufkan1.projectex.client.screen.MatterFurnaceScreen;
import io.github.tufkan1.projectex.client.screen.AlchemyStorageScreen;
import io.github.tufkan1.projectex.client.screen.AutomationScreen;
import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.client.render.DarkMatterPedestalRenderer;
import io.github.tufkan1.projectex.network.AlchemyActionPayload;
import io.github.tufkan1.projectex.network.AlchemyKnowledgePagePayload;
import io.github.tufkan1.projectex.network.AlchemyKnowledgeRequestPayload;
import io.github.tufkan1.projectex.network.AlchemyResultPayload;
import io.github.tufkan1.projectex.network.AlchemySessionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import io.github.tufkan1.projectex.content.ChargeableUtilityItem;
import io.github.tufkan1.projectex.network.UtilityStateAction;
import io.github.tufkan1.projectex.network.UtilityStatePayload;
import io.github.tufkan1.projectex.network.KnowledgeShareDecisionPayload;
import io.github.tufkan1.projectex.network.KnowledgeSharePreviewPayload;
import io.github.tufkan1.projectex.network.KnowledgeShareResultPayload;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-only networking state; screens consume this instead of trusting local calculations. */
@Environment(EnvType.CLIENT)
public final class ProjectEXClient implements ClientModInitializer {
    private static final ClientAlchemySessionState ALCHEMY = new ClientAlchemySessionState();
    private static final ClientKnowledgeBrowserState KNOWLEDGE = new ClientKnowledgeBrowserState();
    private static ClientFavoriteStore favoriteStore;
    private static final KeyMapping.Category UTILITY_CATEGORY =
        KeyMapping.Category.register(ProjectEX.id("utilities"));
    private static final KeyMapping CHARGE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        "key.projectex.utility_charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, UTILITY_CATEGORY
    ));
    private static final KeyMapping MODE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        "key.projectex.utility_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, UTILITY_CATEGORY
    ));
    private static UUID pendingKnowledgeConfirmation;

    @Override
    @SuppressWarnings("deprecation") // Fabric's 26.2 registry remains the supported public hook.
    public void onInitializeClient() {
        favoriteStore = new ClientFavoriteStore(
            FabricLoader.getInstance().getConfigDir().resolve("projectex-favorites.json"));
        KNOWLEDGE.replaceFavorites(favoriteStore.load());
        MenuScreens.register(ProjectEXMenus.TRANSMUTATION, TransmutationScreen::new);
        MenuScreens.register(ProjectEXMenus.EMC_MACHINE, EmcMachineScreen::new);
        MenuScreens.register(ProjectEXMenus.ALCHEMY_STORAGE, AlchemyStorageScreen::new);
        MenuScreens.register(ProjectEXMenus.MATTER_FURNACE, MatterFurnaceScreen::new);
        MenuScreens.register(ProjectEXMenus.AUTOMATION, AutomationScreen::new);
        BlockEntityRendererRegistry.register(
            ProjectEXBlockEntities.DARK_MATTER_PEDESTAL, DarkMatterPedestalRenderer::new);
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
        ClientPlayNetworking.registerGlobalReceiver(KnowledgeSharePreviewPayload.TYPE, (payload, context) ->
            openKnowledgeConfirmation(payload));
        ClientPlayNetworking.registerGlobalReceiver(KnowledgeShareResultPayload.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(payload.success()
                    ? Component.translatable("message.projectex.knowledge_share.confirmed",
                        payload.learned(), payload.total())
                    : Component.translatable("message.projectex.knowledge_share.failed", payload.reason()));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ALCHEMY.close();
            KNOWLEDGE.close();
            pendingKnowledgeConfirmation = null;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CHARGE_KEY.consumeClick()) sendUtilityState(client, UtilityStateAction.CHARGE);
            while (MODE_KEY.consumeClick()) sendUtilityState(client, UtilityStateAction.MODE);
        });
    }

    public static void openKnowledgeConfirmation(KnowledgeSharePreviewPayload payload) {
        Minecraft client = Minecraft.getInstance();
        Screen previous = client.gui.screen();
        if (pendingKnowledgeConfirmation != null) {
            sendKnowledgeDecision(pendingKnowledgeConfirmation, false);
        }
        pendingKnowledgeConfirmation = payload.token();
        Component mode = Component.translatable("item.projectex.knowledge_sharing_book.mode."
            + (payload.mode() == 0 ? "merge" : "replace"));
        client.setScreenAndShow(new ConfirmScreen(accepted -> {
            if (payload.token().equals(pendingKnowledgeConfirmation)) {
                sendKnowledgeDecision(payload.token(), accepted);
                pendingKnowledgeConfirmation = null;
            }
            client.setScreenAndShow(previous);
        }, Component.translatable("screen.projectex.knowledge_share.title"),
            Component.translatable("screen.projectex.knowledge_share.summary", mode,
                payload.added(), payload.removed(), payload.duplicates(), payload.resultSize(),
                payload.ownerId().toString()),
            Component.translatable("screen.projectex.knowledge_share.confirm"),
            Component.translatable("screen.projectex.knowledge_share.cancel")));
    }

    private static void sendKnowledgeDecision(UUID token, boolean accepted) {
        try {
            if (ClientPlayNetworking.canSend(KnowledgeShareDecisionPayload.TYPE)) {
                ClientPlayNetworking.send(new KnowledgeShareDecisionPayload(token, accepted));
            }
        } catch (IllegalStateException exception) {
            ProjectEX.LOGGER.debug("Could not send knowledge sharing decision", exception);
        }
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

    public static boolean toggleFavorite(String itemId) {
        if (!KNOWLEDGE.toggleFavorite(itemId)) {
            return false;
        }
        if (favoriteStore != null && !favoriteStore.save(KNOWLEDGE.snapshot().favorites())) {
            ProjectEX.LOGGER.warn("Could not persist ProjectEX client favorites");
        }
        return true;
    }

    private static void sendUtilityState(
        net.minecraft.client.Minecraft client,
        UtilityStateAction action
    ) {
        if (client.player == null) return;
        try {
            if (!ClientPlayNetworking.canSend(UtilityStatePayload.TYPE)) return;
        } catch (IllegalStateException exception) {
            return;
        }
        int hand = client.player.getMainHandItem().getItem() instanceof ChargeableUtilityItem ? 0
            : client.player.getOffhandItem().getItem() instanceof ChargeableUtilityItem ? 1 : -1;
        if (hand >= 0) {
            try {
                ClientPlayNetworking.send(new UtilityStatePayload(action.ordinal(), hand));
            } catch (IllegalStateException exception) {
                ProjectEX.LOGGER.debug("Could not send utility state keybinding", exception);
            }
        }
    }
}
