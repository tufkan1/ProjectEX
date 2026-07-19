package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.client.ProjectEXClient;
import io.github.tufkan1.projectex.client.screen.TransmutationScreen;
import java.math.BigInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import io.github.tufkan1.projectex.network.KnowledgeSharePreviewPayload;
import java.util.UUID;
import net.minecraft.client.gui.screens.ConfirmScreen;
import io.github.tufkan1.projectex.client.screen.AlchemicalBookScreen;
import io.github.tufkan1.projectex.network.AlchemicalBookViewPayload;
import java.util.List;
import java.util.Optional;

/** End-to-end client/server smoke test for the learn, burn, and create journey. */
@SuppressWarnings("UnstableApiUsage")
public final class ProjectEXClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksDownload();
            singleplayer.getServer().runCommand("give @a minecraft:diamond");
            context.waitFor(ProjectEXClientGameTests::clientHasDiamond);
            singleplayer.getServer().runCommand(
                "execute as @a run projectex transmutation");
            context.waitForScreen(TransmutationScreen.class);

            context.clickScreenButton("Learn held");
            context.waitFor(client -> ProjectEXClient.alchemy().snapshot().lastResponseId() >= 0
                && ProjectEXClient.alchemy().snapshot().lastFailure().isEmpty());
            context.clickScreenButton("Burn held");
            context.waitFor(client -> ProjectEXClient.alchemy().snapshot().lastResponseId() >= 1);
            assertSuccessfulBalance(BigInteger.valueOf(8192), "burn");
            context.waitFor(client -> ProjectEXClient.knowledge().snapshot().entries().stream()
                .anyMatch(entry -> entry.itemId().equals("minecraft:diamond")));

            context.runOnClient(client -> pressButtonContaining(
                client.gui.screen(), "minecraft:diamond"));
            context.clickScreenButton("Create");
            context.waitFor(client -> ProjectEXClient.alchemy().snapshot().lastResponseId() >= 2);
            assertSuccessfulBalance(BigInteger.ZERO, "create");
            context.waitFor(ProjectEXClientGameTests::clientHasDiamond);

            context.runOnClient(client -> ProjectEXClient.openKnowledgeConfirmation(
                new KnowledgeSharePreviewPayload(UUID.randomUUID(), UUID.randomUUID(), 0,
                    3, 0, 1, 4, System.currentTimeMillis() / 1_000L + 120)));
            context.waitForScreen(ConfirmScreen.class);
            context.clickScreenButton("Cancel");
            context.waitForScreen(TransmutationScreen.class);

            context.runOnClient(client -> {
                ProjectEXClient.alchemicalBook().open(new AlchemicalBookViewPayload(
                    UUID.randomUUID(), -1, 0, true, "10000", "", List.of(), Optional.empty()));
                client.setScreenAndShow(new AlchemicalBookScreen());
            });
            context.waitForScreen(AlchemicalBookScreen.class);
            context.runOnClient(client -> {
                assertButtonContaining(client.gui.screen(), "Save here");
                assertButtonContaining(client.gui.screen(), "Go back");
                client.setScreenAndShow(null);
            });
        }
    }

    private static boolean clientHasDiamond(net.minecraft.client.Minecraft client) {
        return client.player != null
            && client.player.getInventory().getNonEquipmentItems().stream()
                .anyMatch(stack -> stack.is(net.minecraft.world.item.Items.DIAMOND));
    }

    private static void assertSuccessfulBalance(BigInteger expected, String operation) {
        var snapshot = ProjectEXClient.alchemy().snapshot();
        if (!snapshot.balance().equals(expected) || snapshot.lastFailure().isPresent()) {
            throw new AssertionError(operation + " failed: balance=" + snapshot.balance()
                + ", failure=" + snapshot.lastFailure());
        }
    }

    private static void pressButtonContaining(Screen screen, String text) {
        if (screen == null) {
            throw new AssertionError("No client screen is open");
        }
        screen.children().stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.visible && button.getMessage().getString().contains(text))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No visible button contains: " + text))
            .onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    private static void assertButtonContaining(Screen screen, String text) {
        if (screen == null || screen.children().stream().filter(Button.class::isInstance)
            .map(Button.class::cast).noneMatch(button -> button.visible
                && button.getMessage().getString().contains(text))) {
            throw new AssertionError("No visible button contains: " + text);
        }
    }
}
