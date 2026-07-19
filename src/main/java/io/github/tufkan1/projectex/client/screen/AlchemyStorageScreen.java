package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Accessible paged inventory over pinned original ProjectE/ProjectExpansion panels. */
@Environment(EnvType.CLIENT)
public final class AlchemyStorageScreen extends AbstractContainerScreen<AlchemyStorageMenu> {
    private Button previousPage;
    private Button nextPage;
    private Button access;

    public AlchemyStorageScreen(AlchemyStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 248);
        inventoryLabelY = 154;
    }

    @Override protected void init() {
        super.init();
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"),
            ignored -> send(100)).bounds(leftPos + 30, topPos + 18, 20, 16).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"),
            ignored -> send(101)).bounds(leftPos + 52, topPos + 18, 20, 16).build());
        access = addRenderableWidget(Button.builder(accessLabel(), ignored -> send(102))
            .bounds(leftPos + 118, topPos + 18, 50, 16).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        previousPage.active = menu.pageCount() > 1;
        nextPage.active = menu.pageCount() > 1;
        access.setMessage(accessLabel());
    }

    @Override public void extractBackground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        net.minecraft.resources.Identifier texture = switch (menu.kind()) {
            case CONDENSER_MK1 -> ProjectEXGuiTextures.CONDENSER_1;
            case CONDENSER_MK2 -> ProjectEXGuiTextures.CONDENSER_2;
            case CONDENSER_MK3 -> menu.inputPage()
                ? ProjectEXGuiTextures.CONDENSER_3_INPUT : ProjectEXGuiTextures.CONDENSER_3_OUTPUT;
            default -> ProjectEXGuiTextures.ALCHEMICAL_CHEST;
        };
        ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0x30111518);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF6A9A91);
        graphics.fill(leftPos + 7, topPos + 39, leftPos + 169, topPos + 148, 0xFF22292B);
        if (menu.kind().condenser()) {
            graphics.fill(leftPos + 7, topPos + 17, leftPos + 25, topPos + 35, 0xFF35443F);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE7FFF9, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE7FFF9, false);
        graphics.text(font, Component.translatable(menu.kind().condenser()
            ? (menu.inputPage() ? "screen.projectex.storage.inputs" : "screen.projectex.storage.outputs")
            : "screen.projectex.storage.contents"), 8, 142, 0xFFAED7CD, false);
    }

    @Override public Component getNarrationMessage() {
        return Component.translatable("screen.projectex.storage.narration", title, menu.page() + 1);
    }

    private void send(int id) {
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component accessLabel() {
        return Component.translatable(menu.publicAccess()
            ? "screen.projectex.machine_access.public" : "screen.projectex.machine_access.private");
    }
}
