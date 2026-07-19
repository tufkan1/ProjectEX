package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.matter.MatterTier;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Native ProjectE matter-furnace screen and progress artwork. */
@Environment(EnvType.CLIENT)
public final class MatterFurnaceScreen extends AbstractContainerScreen<MatterFurnaceMenu> {
    public MatterFurnaceScreen(MatterFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.tier() == MatterTier.RED ? 209 : 178, 165);
        titleLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        boolean red = menu.tier() == MatterTier.RED;
        Identifier texture = red ? ProjectEXGuiTextures.RED_FURNACE : ProjectEXGuiTextures.DARK_FURNACE;
        ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, imageWidth, imageHeight);
        int progress = menu.cookPixels(24);
        if (progress > 0) ProjectEXGuiTextures.region(graphics, texture,
            leftPos + (red ? 88 : 73), topPos + 34, red ? 210 : 179, 14,
            progress, 16, 256, 256);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) { }

    @Override public Component getNarrationMessage() {
        return Component.translatable("screen.projectex.matter_furnace.narration", title,
            menu.cookPixels(100), menu.litPixels(100));
    }
}
