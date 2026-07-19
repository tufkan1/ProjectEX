package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Native-sized ProjectE collector/relay UI with ProjectEX controls outside the source panel. */
@Environment(EnvType.CLIENT)
public final class EmcMachineScreen extends AbstractContainerScreen<EmcMachineMenu> {
    private static final int CONTROL_HEIGHT = 22;
    private Button redstone;
    private Button access;

    public EmcMachineScreen(EmcMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, sourceWidth(menu.tier()), sourceHeight(menu.tier()) + CONTROL_HEIGHT);
        titleLabelX = -10_000;
        inventoryLabelY = -10_000;
    }

    @Override protected void init() {
        super.init();
        int y = topPos + sourceHeight(menu.tier()) + 3;
        int half = (imageWidth - 18) / 2;
        redstone = addRenderableWidget(Button.builder(redstoneLabel(), ignored -> sendButton(0))
            .bounds(leftPos + 6, y, half, 16).build());
        access = addRenderableWidget(Button.builder(accessLabel(), ignored -> sendButton(1))
            .bounds(leftPos + 12 + half, y, half, 16).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        redstone.setMessage(redstoneLabel());
        access.setMessage(accessLabel());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        MachineTier tier = menu.tier();
        Identifier texture = relay(tier) ? ProjectEXGuiTextures.relay(tier.level())
            : ProjectEXGuiTextures.collector(tier.level());
        int width = sourceWidth(tier);
        int height = sourceHeight(tier);
        ProjectEXGuiTextures.draw(graphics, texture, leftPos, topPos, width, height);
        drawEmc(graphics, texture, tier);
        graphics.fill(leftPos, topPos + height, leftPos + imageWidth,
            topPos + height + CONTROL_HEIGHT, 0xE0181B20);
        graphics.outline(leftPos, topPos + height, imageWidth, CONTROL_HEIGHT, 0xFF777777);
    }

    private void drawEmc(GuiGraphicsExtractor graphics, Identifier texture, MachineTier tier) {
        int level = Math.min(3, tier.level());
        int stored = Math.max(0, menu.storedEmc());
        int capacity = Math.max(1, menu.capacity());
        if (relay(tier)) {
            int x = switch (level) { case 1 -> 64; case 2 -> 86; default -> 105; };
            int v = switch (level) { case 1 -> 177; case 2 -> 183; default -> 195; };
            int pixels = (int) Math.min(102L, (long) stored * 102 / capacity);
            if (pixels > 0) ProjectEXGuiTextures.region(graphics, texture, leftPos + x, topPos + 6,
                30, v, pixels, 10, 256, 256);
        } else {
            int shift = switch (level) { case 1 -> 0; case 2 -> 16; default -> 34; };
            int v = switch (level) { case 1 -> 166; case 2 -> 191; default -> 209; };
            int pixels = (int) Math.min(48L, (long) stored * 48 / capacity);
            if (pixels > 0) ProjectEXGuiTextures.region(graphics, texture, leftPos + 64 + shift,
                topPos + 18, 0, v, pixels, 10, 256, 256);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MachineTier tier = menu.tier();
        int level = Math.min(3, tier.level());
        int x = relay(tier) ? switch (level) { case 1 -> 88; case 2 -> 107; default -> 125; }
            : 60 + switch (level) { case 1 -> 0; case 2 -> 16; default -> 34; };
        int y = relay(tier) ? switch (level) { case 1 -> 24; case 2 -> 25; default -> 39; } : 32;
        String emc = menu.storedEmc() + " / " + menu.capacity();
        graphics.text(font, Component.literal(emc), x - font.width(emc) / 2, y, 0xFF404040, false);
    }

    @Override public Component getNarrationMessage() {
        return Component.translatable("screen.projectex.machine_narration", title,
            menu.storedEmc(), menu.capacity());
    }

    private void sendButton(int id) {
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component redstoneLabel() {
        return Component.translatable("screen.projectex.machine_redstone."
            + menu.redstoneMode().name().toLowerCase(java.util.Locale.ROOT));
    }

    private Component accessLabel() {
        return Component.translatable(menu.publicAccess()
            ? "screen.projectex.machine_access.public" : "screen.projectex.machine_access.private");
    }

    private static boolean relay(MachineTier tier) { return tier.type() == MachineTier.MachineType.RELAY; }
    private static int sourceWidth(MachineTier tier) {
        int level = Math.min(3, tier.level());
        if (relay(tier)) return switch (level) { case 1 -> 175; case 2 -> 193; default -> 212; };
        return switch (level) { case 1 -> 176; case 2 -> 200; default -> 218; };
    }
    private static int sourceHeight(MachineTier tier) {
        int level = Math.min(3, tier.level());
        if (relay(tier)) return switch (level) { case 1 -> 176; case 2 -> 182; default -> 194; };
        return level == 1 ? 166 : 165;
    }
}
