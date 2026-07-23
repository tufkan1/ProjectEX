package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.client.ProjectEXClient;
import io.github.tufkan1.projectex.client.InputCompat;
import io.github.tufkan1.projectex.client.screen.TransmutationScreen;
import java.math.BigInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import io.github.tufkan1.projectex.network.KnowledgeSharePreviewPayload;
import java.util.UUID;
import net.minecraft.client.gui.screens.ConfirmScreen;
import io.github.tufkan1.projectex.client.screen.AlchemicalBookScreen;
import io.github.tufkan1.projectex.network.AlchemicalBookViewPayload;
import java.util.List;
import java.util.Optional;
import io.github.tufkan1.projectex.client.screen.AlchemyStorageScreen;
import io.github.tufkan1.projectex.client.screen.EmcMachineScreen;
import io.github.tufkan1.projectex.client.screen.MatterFurnaceScreen;
import io.github.tufkan1.projectex.client.screen.ProjectEXConfigScreen;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import io.github.tufkan1.projectex.storage.StorageKind;
import io.github.tufkan1.projectex.compat.jei.ProjectEXJeiPlugin;
import net.fabricmc.loader.api.FabricLoader;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.component.ArcaneTabletState;
import io.github.tufkan1.projectex.network.OpenArcaneTabletPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;

/** End-to-end client/server smoke test for the learn, burn, and create journey. */
@SuppressWarnings("UnstableApiUsage")
public final class ProjectEXClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksDownload();
            singleplayer.getServer().runCommand("give @a minecraft:diamond");
            context.waitFor(ProjectEXClientGameTests::clientHasDiamond);
            verifyJeiArcaneTransferWhenPresent(context, singleplayer);
            context.waitFor(client -> ProjectEXClient.emcTooltips().find("minecraft:diamond")
                .filter("8192"::equals).isPresent());
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

            verifyEverySourcePanelRenders(context);
        }
    }

    private static void verifyJeiArcaneTransferWhenPresent(
        ClientGameTestContext context, TestSingleplayerContext singleplayer
    ) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return;
        singleplayer.getServer().runCommand(
            "give @a projectex:arcane_tablet[projectex:arcane_tablet_state=crafting]");
        context.waitFor(client -> client.player != null
            && client.player.getInventory().getNonEquipmentItems().stream().anyMatch(stack ->
                stack.is(ProjectEXItems.ARCANE_TABLET.item())
                    && stack.getOrDefault(ProjectEXComponents.ARCANE_TABLET_STATE,
                        ArcaneTabletState.DEFAULT).mode() == ArcaneTabletState.Mode.CRAFTING));
        context.runOnClient(client -> ClientPlayNetworking.send(OpenArcaneTabletPayload.INSTANCE));
        context.waitForScreen(CraftingScreen.class);
        context.waitFor(client -> {
            if (client.player == null) return false;
            return ProjectEXJeiPlugin.hasCraftingTransfer(client.player.containerMenu);
        });
        context.runOnClient(client -> client.setScreenAndShow(null));
    }

    private static void verifyEverySourcePanelRenders(ClientGameTestContext context) {
        openMachine(context, MachineTier.COLLECTOR_MK1);
        openMachine(context, MachineTier.COLLECTOR_MK2);
        openMachine(context, MachineTier.COLLECTOR_MK3);
        openMachine(context, MachineTier.RELAY_MK1);
        openMachine(context, MachineTier.RELAY_MK2);
        openMachine(context, MachineTier.RELAY_MK3);
        openMachine(context, MachineTier.COLLECTOR_FINAL);

        openStorage(context, StorageKind.ALCHEMICAL_CHEST, false);
        openStorage(context, StorageKind.ADVANCED_ALCHEMICAL_CHEST, false);
        openStorage(context, StorageKind.ALCHEMICAL_BAG, false);
        openStorage(context, StorageKind.CONDENSER_MK1, false);
        openStorage(context, StorageKind.CONDENSER_MK2, false);
        openStorage(context, StorageKind.CONDENSER_MK3, false);
        openStorage(context, StorageKind.CONDENSER_MK3, true);

        openFurnace(context, 0);
        openFurnace(context, 1);
        context.runOnClient(client -> client.setScreenAndShow(
            new ProjectEXConfigScreen(client.gui.screen())));
        context.waitForScreen(ProjectEXConfigScreen.class);
        context.runOnClient(client -> client.setScreenAndShow(null));
    }

    private static void openMachine(ClientGameTestContext context, MachineTier tier) {
        context.runOnClient(client -> {
            if (client.player == null) throw new AssertionError("Client player is missing");
            var inventory = client.player.getInventory();
            var menu = new EmcMachineMenu(0, inventory, tier.ordinal());
            client.setScreenAndShow(new EmcMachineScreen(menu, inventory, Component.empty()));
        });
        context.waitForScreen(EmcMachineScreen.class);
    }

    private static void openStorage(ClientGameTestContext context, StorageKind kind,
                                    boolean outputView) {
        context.runOnClient(client -> {
            if (client.player == null) throw new AssertionError("Client player is missing");
            var inventory = client.player.getInventory();
            var menu = new AlchemyStorageMenu(0, inventory,
                AlchemyStorageMenu.openingData(kind, outputView));
            client.setScreenAndShow(new AlchemyStorageScreen(menu, inventory, Component.empty()));
        });
        context.waitForScreen(AlchemyStorageScreen.class);
    }

    private static void openFurnace(ClientGameTestContext context, int tier) {
        context.runOnClient(client -> {
            if (client.player == null) throw new AssertionError("Client player is missing");
            var inventory = client.player.getInventory();
            var menu = new MatterFurnaceMenu(0, inventory, tier);
            client.setScreenAndShow(new MatterFurnaceScreen(menu, inventory, Component.empty()));
        });
        context.waitForScreen(MatterFurnaceScreen.class);
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
            .onPress(new KeyEvent(InputCompat.KEY_ENTER, 0, 0));
    }

    private static void assertButtonContaining(Screen screen, String text) {
        if (screen == null || screen.children().stream().filter(Button.class::isInstance)
            .map(Button.class::cast).noneMatch(button -> button.visible
                && button.getMessage().getString().contains(text))) {
            throw new AssertionError("No visible button contains: " + text);
        }
    }
}
