package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.storage.StorageKind;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Unmodified source-sized ProjectE/ProjectExpansion storage panels. */
@Environment(EnvType.CLIENT)
public final class AlchemyStorageScreen extends AbstractContainerScreen<AlchemyStorageMenu> {
    public AlchemyStorageScreen(AlchemyStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, sourceWidth(menu), sourceHeight(menu));
        titleLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        Identifier texture = switch (menu.kind()) {
            case CONDENSER_MK1 -> ProjectEXGuiTextures.CONDENSER_1;
            case CONDENSER_MK2 -> ProjectEXGuiTextures.CONDENSER_2;
            case CONDENSER_MK3 -> menu.outputView()
                ? ProjectEXGuiTextures.CONDENSER_3_OUTPUT : ProjectEXGuiTextures.CONDENSER_3_INPUT;
            default -> ProjectEXGuiTextures.ALCHEMICAL_CHEST;
        };
        int width = sourceWidth(menu);
        int height = sourceHeight(menu);
        if (menu.kind() == StorageKind.CONDENSER_MK3 && menu.outputView()) {
            ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, width, height, width, height);
        } else {
            ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, width, height);
        }
    }

    private static int sourceWidth(AlchemyStorageMenu menu) {
        return menu.kind() == StorageKind.CONDENSER_MK3 && menu.outputView() ? 382 : 255;
    }

    private static int sourceHeight(AlchemyStorageMenu menu) {
        if (menu.kind() == StorageKind.CONDENSER_MK3 && menu.outputView()) return 252;
        return menu.kind().condenser() ? 233 : 230;
    }
}
