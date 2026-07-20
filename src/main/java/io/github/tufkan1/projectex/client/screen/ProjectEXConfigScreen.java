package io.github.tufkan1.projectex.client.screen;

import io.github.tufkan1.projectex.config.ProjectEXConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dependency-light Mod Menu screen for ProjectEX's client-only preferences. */
@Environment(EnvType.CLIENT)
public final class ProjectEXConfigScreen extends Screen {
    private final Screen parent;
    private boolean rememberFavorites;
    private boolean showEmcTooltips;
    private boolean compactEmcNumbers;
    private boolean focusTransmutationSearch;

    public ProjectEXConfigScreen(Screen parent) {
        super(Component.translatable("screen.projectex.config.title"));
        this.parent = parent;
        load(ProjectEXConfig.clientOptions());
    }

    @Override protected void init() {
        int left = (width - 310) / 2;
        int top = Math.max(34, (height - 190) / 2);
        addRenderableWidget(toggle(left, top + 28, "remember_favorites",
            () -> rememberFavorites, value -> rememberFavorites = value));
        addRenderableWidget(toggle(left, top + 54, "show_emc_tooltips",
            () -> showEmcTooltips, value -> showEmcTooltips = value));
        addRenderableWidget(toggle(left, top + 80, "compact_emc",
            () -> compactEmcNumbers, value -> compactEmcNumbers = value));
        addRenderableWidget(toggle(left, top + 106, "focus_search",
            () -> focusTransmutationSearch, value -> focusTransmutationSearch = value));
        addRenderableWidget(Button.builder(Component.translatable("screen.projectex.config.reset"),
            button -> {
                load(ProjectEXConfig.ClientOptions.DEFAULT);
                rebuildWidgets();
            }).bounds(left, top + 145, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
            button -> closeToParent()).bounds(left + 102, top + 145, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            button -> save()).bounds(left + 204, top + 145, 106, 20).build());
    }

    @Override public void extractRenderState(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, Math.max(18, (height - 190) / 2),
            0xFFFFFFFF);
    }

    @Override public void onClose() { closeToParent(); }

    private Button toggle(int x, int y, String key, BooleanValue getter, BooleanSetter setter) {
        return Button.builder(message(key, getter.get()), button -> {
            setter.set(!getter.get());
            button.setMessage(message(key, getter.get()));
        }).bounds(x, y, 310, 20).build();
    }

    private static Component message(String key, boolean enabled) {
        return Component.translatable("screen.projectex.config." + key,
            Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private void save() {
        ProjectEXConfig.saveClientOptions(new ProjectEXConfig.ClientOptions(
            rememberFavorites, showEmcTooltips, compactEmcNumbers, focusTransmutationSearch));
        closeToParent();
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }

    private void load(ProjectEXConfig.ClientOptions options) {
        rememberFavorites = options.rememberFavorites();
        showEmcTooltips = options.showEmcTooltips();
        compactEmcNumbers = options.compactEmcNumbers();
        focusTransmutationSearch = options.focusTransmutationSearch();
    }

    @FunctionalInterface private interface BooleanValue { boolean get(); }
    @FunctionalInterface private interface BooleanSetter { void set(boolean value); }
}
