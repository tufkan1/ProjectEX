package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Narrated furnace screen over the original ProjectE matter-furnace panel. */
@Environment(EnvType.CLIENT)
public final class MatterFurnaceScreen extends AbstractContainerScreen<MatterFurnaceMenu> {
    public MatterFurnaceScreen(MatterFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 214, 188);
        inventoryLabelX = 26;
        inventoryLabelY = 94;
    }

    @Override public void extractBackground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int accent = menu.tier() == io.github.tufkan1.projectex.matter.MatterTier.RED
            ? 0xFFD34C4C : 0xFF6D5A86;
        ProjectEXGuiTextures.draw(graphics,
            menu.tier() == io.github.tufkan1.projectex.matter.MatterTier.RED
                ? ProjectEXGuiTextures.RED_FURNACE : ProjectEXGuiTextures.DARK_FURNACE,
            leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0x30121218);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, accent);
        graphics.fill(leftPos + 15, topPos + 26, leftPos + 35, topPos + 48, 0xFF29232F);
        graphics.fill(leftPos + 15, topPos + 53, leftPos + 35, topPos + 75, 0xFF29232F);
        graphics.fill(leftPos + 40, topPos + 24, leftPos + 208, topPos + 68, 0xFF211D27);
        graphics.fill(leftPos + 39, topPos + 76, leftPos + 207, topPos + 82, 0xFF30283A);
        graphics.fill(leftPos + 39, topPos + 76,
            leftPos + 39 + menu.cookPixels(168), topPos + 82, accent);
        graphics.fill(leftPos + 16, topPos + 78, leftPos + 34, topPos + 84, 0xFF30283A);
        graphics.fill(leftPos + 16, topPos + 78,
            leftPos + 16 + menu.litPixels(18), topPos + 84, 0xFFFFA52E);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE7DDF0, false);
        graphics.text(font, Component.translatable(
            "screen.projectex.matter_furnace.outputs", menu.tier().furnaceOutputSlots()
        ), 126, 8, 0xFFCCBBDD, false);
    }

    @Override public Component getNarrationMessage() {
        return Component.translatable(
            "screen.projectex.matter_furnace.narration", title,
            menu.cookPixels(100), menu.litPixels(100)
        );
    }
}
