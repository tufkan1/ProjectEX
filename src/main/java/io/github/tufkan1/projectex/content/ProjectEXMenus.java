package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Common menu type registration. */
public final class ProjectEXMenus {
    public static final MenuType<TransmutationMenu> TRANSMUTATION = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("transmutation"),
        new MenuType<>(TransmutationMenu::new, FeatureFlags.VANILLA_SET)
    );

    private ProjectEXMenus() {
    }

    public static void register() {
        ProjectEX.LOGGER.debug("Registered ProjectEX menu types: {}", TRANSMUTATION);
    }
}
