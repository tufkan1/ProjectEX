package io.github.tufkan1.projectex.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Pinned MIT-licensed upstream GUI sheets; custom controls remain accessible overlays. */
final class ProjectEXGuiTextures {
    static final Identifier TRANSMUTATION = projecte("transmute");
    static final Identifier ALCHEMICAL_CHEST = projecte("alchchest");
    static final Identifier CONDENSER_1 = projecte("condenser");
    static final Identifier CONDENSER_2 = projecte("condenser_mk2");
    static final Identifier CONDENSER_3_INPUT = expansion("condenser_mk3_input");
    static final Identifier CONDENSER_3_OUTPUT = expansion("condenser_mk3_output");
    static final Identifier DARK_FURNACE = projecte("dmfurnace");
    static final Identifier RED_FURNACE = projecte("rmfurnace");

    private ProjectEXGuiTextures() {}

    static Identifier collector(int level) { return projecte("collector" + Math.min(3, level)); }
    static Identifier relay(int level) { return projecte("relay" + Math.min(3, level)); }

    static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        draw(graphics, texture, x, y, width, height, 256, 256);
    }

    static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                     int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0,
            width, height, textureWidth, textureHeight);
    }

    static void region(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                       int u, int v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v,
            width, height, textureWidth, textureHeight);
    }

    private static Identifier projecte(String name) {
        return Identifier.fromNamespaceAndPath("projecte", "textures/gui/" + name + ".png");
    }
    private static Identifier expansion(String name) {
        return Identifier.fromNamespaceAndPath("projectexpansion", "textures/gui/" + name + ".png");
    }
}
