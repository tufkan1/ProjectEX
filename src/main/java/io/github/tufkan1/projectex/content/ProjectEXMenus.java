package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
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
    public static final MenuType<EmcMachineMenu> EMC_MACHINE = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("emc_machine"),
        new MenuType<>(EmcMachineMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<AlchemyStorageMenu> ALCHEMY_STORAGE = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("alchemy_storage"),
        new MenuType<>(AlchemyStorageMenu::new, FeatureFlags.VANILLA_SET)
    );

    private ProjectEXMenus() {
    }

    public static void register() {
        ProjectEX.LOGGER.debug("Registered ProjectEX menu types: {}", TRANSMUTATION);
    }
}
