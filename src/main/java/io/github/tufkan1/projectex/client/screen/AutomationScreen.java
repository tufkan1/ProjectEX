package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.AutomationMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** ProjectEX automation configuration screen (the source mod does not expose this as a container). */
@Environment(EnvType.CLIENT)
public final class AutomationScreen extends AbstractContainerScreen<AutomationMenu> {
    private Button publicInsert;
    private Button insertMode;
    private Button extractMode;

    public AutomationScreen(AutomationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = 72;
    }

    @Override protected void init() {
        super.init();
        publicInsert = addRenderableWidget(Button.builder(publicLabel(), ignored -> send(0))
            .bounds(leftPos + 8, topPos + 50, 52, 16).build());
        insertMode = addRenderableWidget(Button.builder(insertLabel(), ignored -> send(1))
            .bounds(leftPos + 62, topPos + 50, 52, 16).build());
        extractMode = addRenderableWidget(Button.builder(extractLabel(), ignored -> send(2))
            .bounds(leftPos + 116, topPos + 50, 52, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.projectex.automation.add_insert"),
            ignored -> send(3)).bounds(leftPos + 8, topPos + 68, 78, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.projectex.automation.add_extract"),
            ignored -> send(4)).bounds(leftPos + 90, topPos + 68, 78, 14).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        publicInsert.setMessage(publicLabel());
        insertMode.setMessage(insertLabel());
        extractMode.setMessage(extractLabel());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE10151B);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF57B9C9);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE8FAFF, false);
        graphics.text(font, Component.translatable("screen.projectex.automation.tier",
            menu.tier().id(), menu.tier().level()), 8, 20, 0xFFB8E9F2, false);
        graphics.text(font, Component.translatable("screen.projectex.automation.filters",
            menu.insertEntries(), menu.extractEntries()), 8, 34, 0xFFFFFFFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE8FAFF, false);
    }

    @Override public Component getNarrationMessage() {
        return Component.translatable("screen.projectex.automation.narration", title,
            menu.tier().id(), menu.insertEntries(), menu.extractEntries());
    }

    private void send(int id) {
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
    private Component publicLabel() {
        return Component.translatable(menu.publicInsert()
            ? "screen.projectex.automation.public_on" : "screen.projectex.automation.public_off");
    }
    private Component insertLabel() {
        return Component.translatable(menu.insertMode() == 0
            ? "screen.projectex.automation.allow_in" : "screen.projectex.automation.deny_in");
    }
    private Component extractLabel() {
        return Component.translatable(menu.extractMode() == 0
            ? "screen.projectex.automation.allow_out" : "screen.projectex.automation.deny_out");
    }
}
