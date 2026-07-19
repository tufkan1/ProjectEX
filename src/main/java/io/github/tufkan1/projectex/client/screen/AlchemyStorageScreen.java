package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.storage.StorageKind;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Source-sized ProjectE/ProjectExpansion storage panels with controls in a separate strip. */
@Environment(EnvType.CLIENT)
public final class AlchemyStorageScreen extends AbstractContainerScreen<AlchemyStorageMenu> {
    private static final int CONTROL_HEIGHT = 22;
    private Button previousPage;
    private Button nextPage;
    private Button access;

    public AlchemyStorageScreen(AlchemyStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, sourceWidth(menu.kind()), sourceHeight(menu.kind()) + CONTROL_HEIGHT);
        titleLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override protected void init() {
        super.init();
        int y = topPos + sourceHeight(menu.kind()) + 3;
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> send(100))
            .bounds(leftPos + 6, y, 20, 16).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> send(101))
            .bounds(leftPos + 28, y, 20, 16).build());
        access = addRenderableWidget(Button.builder(accessLabel(), ignored -> send(102))
            .bounds(leftPos + imageWidth - 72, y, 66, 16).build());
        access.visible = menu.kind() != StorageKind.ALCHEMICAL_BAG;
    }

    @Override protected void containerTick() {
        super.containerTick();
        previousPage.active = menu.pageCount() > 1;
        nextPage.active = menu.pageCount() > 1;
        access.setMessage(accessLabel());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        StorageKind kind = menu.kind();
        Identifier texture = switch (kind) {
            case CONDENSER_MK1 -> ProjectEXGuiTextures.CONDENSER_1;
            case CONDENSER_MK2 -> ProjectEXGuiTextures.CONDENSER_2;
            case CONDENSER_MK3 -> menu.inputPage()
                ? ProjectEXGuiTextures.CONDENSER_3_INPUT : ProjectEXGuiTextures.CONDENSER_3_OUTPUT;
            default -> ProjectEXGuiTextures.ALCHEMICAL_CHEST;
        };
        if (kind == StorageKind.CONDENSER_MK3 && menu.inputPage()) {
            ProjectEXGuiTextures.draw(graphics, texture, leftPos + 54, topPos, 255, 233);
        } else if (kind == StorageKind.CONDENSER_MK3) {
            ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, 382, 252, 382, 252);
        } else {
            ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos,
                sourceWidth(kind), sourceHeight(kind));
        }
        int stripY = topPos + sourceHeight(kind);
        graphics.fill(leftPos, stripY, leftPos + imageWidth, stripY + CONTROL_HEIGHT, 0xE0181B20);
        graphics.outline(leftPos, stripY, imageWidth, CONTROL_HEIGHT, 0xFF777777);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component page = Component.translatable(menu.kind().condenser()
            ? (menu.inputPage() ? "screen.projectex.storage.inputs" : "screen.projectex.storage.outputs")
            : "screen.projectex.storage.contents");
        graphics.text(font, Component.literal((menu.page() + 1) + "/" + menu.pageCount() + " ").append(page),
            54, sourceHeight(menu.kind()) + 7, 0xFFE8E8E8, false);
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

    private static int sourceWidth(StorageKind kind) { return kind == StorageKind.CONDENSER_MK3 ? 382 : 255; }
    private static int sourceHeight(StorageKind kind) {
        if (kind == StorageKind.CONDENSER_MK3) return 252;
        return kind.condenser() ? 233 : 230;
    }
}
