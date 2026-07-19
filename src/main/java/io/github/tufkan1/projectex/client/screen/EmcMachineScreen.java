package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

/** Accessible machine UI over the original ProjectE collector/relay panels. */
@Environment(EnvType.CLIENT)
public final class EmcMachineScreen extends AbstractContainerScreen<EmcMachineMenu> {
    private Button redstone;
    private Button access;

    public EmcMachineScreen(EmcMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        redstone = addRenderableWidget(Button.builder(redstoneLabel(), ignored -> sendButton(0))
            .bounds(leftPos + 8, topPos + 67, 78, 14).build());
        access = addRenderableWidget(Button.builder(accessLabel(), ignored -> sendButton(1))
            .bounds(leftPos + 90, topPos + 67, 78, 14).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        redstone.setMessage(redstoneLabel());
        access.setMessage(accessLabel());
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        net.minecraft.resources.Identifier texture = menu.tier().type()
            == io.github.tufkan1.projectex.machine.MachineTier.MachineType.RELAY
            ? ProjectEXGuiTextures.relay(menu.tier().level())
            : ProjectEXGuiTextures.collector(menu.tier().level());
        ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0x30121218);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF7A58A6);
        graphics.fill(leftPos + 52, topPos + 34, leftPos + 70, topPos + 52, 0xFF2A2530);
        graphics.fill(leftPos + 106, topPos + 34, leftPos + 124, topPos + 52, 0xFF2A2530);
        int width = menu.capacity() <= 0
            ? 0
            : (int) ((long) Math.max(0, menu.storedEmc()) * 116 / menu.capacity());
        graphics.fill(leftPos + 30, topPos + 58, leftPos + 146, topPos + 66, 0xFF292233);
        graphics.fill(leftPos + 30, topPos + 58, leftPos + 30 + width, topPos + 66, 0xFF9D6BDE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEEDCFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFEEDCFF, false);
        graphics.text(font, Component.translatable(
            "screen.projectex.machine_emc", menu.storedEmc(), menu.capacity()),
            30, 21, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable(
            "screen.projectex.machine_tier", menu.tier().name()),
            100, 8, 0xFFBFA8D6, false);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.translatable(
            "screen.projectex.machine_narration",
            title,
            menu.storedEmc(),
            menu.capacity()
        );
    }

    private void sendButton(int id) {
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component redstoneLabel() {
        return Component.translatable(
            "screen.projectex.machine_redstone." + menu.redstoneMode().name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    private Component accessLabel() {
        return Component.translatable(menu.publicAccess()
            ? "screen.projectex.machine_access.public"
            : "screen.projectex.machine_access.private");
    }
}
