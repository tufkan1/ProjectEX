package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import io.github.tufkan1.projectex.menu.AutomationMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

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
        new ExtendedMenuType<>(EmcMachineMenu::new, ByteBufCodecs.VAR_INT)
    );
    public static final MenuType<AlchemyStorageMenu> ALCHEMY_STORAGE = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("alchemy_storage"),
        new ExtendedMenuType<>(AlchemyStorageMenu::new, ByteBufCodecs.VAR_INT)
    );
    public static final MenuType<MatterFurnaceMenu> MATTER_FURNACE = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("matter_furnace"),
        new ExtendedMenuType<>(MatterFurnaceMenu::new, ByteBufCodecs.VAR_INT)
    );
    public static final MenuType<AutomationMenu> AUTOMATION = Registry.register(
        BuiltInRegistries.MENU,
        ProjectEX.id("automation"),
        new ExtendedMenuType<>(AutomationMenu::new, ByteBufCodecs.VAR_INT)
    );

    private ProjectEXMenus() {
    }

    public static void register() {
        ProjectEX.LOGGER.debug("Registered ProjectEX menu types: {}", TRANSMUTATION);
    }
}
