package io.github.tufkan1.projectex.compat.jei;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.menu.ArcaneCraftingMenu;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Optional JEI bridge for the portable 3x3 Arcane Crafting menu. */
@JeiPlugin
public final class ProjectEXJeiPlugin implements IModPlugin {
    private static final int FIRST_RECIPE_SLOT = 1;
    private static final int RECIPE_SLOT_COUNT = 9;
    private static final int FIRST_INVENTORY_SLOT = 10;
    private static final int INVENTORY_SLOT_COUNT = 36;
    private static volatile IJeiRuntime runtime;

    @Override public Identifier getPluginUid() {
        return ProjectEX.id("jei");
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArcaneCraftingTransferInfo());
        ProjectEX.LOGGER.info("ProjectEX JEI recipe transfer ready");
    }

    @Override public void onRuntimeAvailable(IJeiRuntime availableRuntime) {
        runtime = availableRuntime;
    }

    @Override public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Testable runtime proof that JEI resolved the handler used to render its transfer button. */
    public static boolean hasCraftingTransfer(AbstractContainerMenu menu) {
        IJeiRuntime current = runtime;
        if (current == null) return false;
        var category = current.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
        return current.getRecipeTransferManager().getRecipeTransferHandler(menu, category).isPresent();
    }

    private static final class ArcaneCraftingTransferInfo implements
        IRecipeTransferInfo<ArcaneCraftingMenu, RecipeHolder<CraftingRecipe>> {

        @Override public Class<? extends ArcaneCraftingMenu> getContainerClass() {
            return ArcaneCraftingMenu.class;
        }

        @Override public Optional<MenuType<ArcaneCraftingMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override public boolean canHandle(ArcaneCraftingMenu menu,
                                           RecipeHolder<CraftingRecipe> recipe) {
            return menu.slots.size() >= FIRST_INVENTORY_SLOT + INVENTORY_SLOT_COUNT;
        }

        @Override public List<Slot> getRecipeSlots(ArcaneCraftingMenu menu,
                                                   RecipeHolder<CraftingRecipe> recipe) {
            return List.copyOf(menu.slots.subList(
                FIRST_RECIPE_SLOT, FIRST_RECIPE_SLOT + RECIPE_SLOT_COUNT));
        }

        @Override public List<Slot> getInventorySlots(ArcaneCraftingMenu menu,
                                                      RecipeHolder<CraftingRecipe> recipe) {
            return List.copyOf(menu.slots.subList(
                FIRST_INVENTORY_SLOT, FIRST_INVENTORY_SLOT + INVENTORY_SLOT_COUNT));
        }
    }
}
