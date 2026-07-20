package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.component.BagItemState;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.storage.StorageKind;
import io.github.tufkan1.projectex.internal.storage.BagInventorySavedData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** A color-keyed, per-stack identity-safe portable alchemical inventory. */
public final class AlchemicalBagItem extends Item {
    public static final int SLOTS = 104;
    private final DyeColor color;

    public AlchemicalBagItem(Properties properties, DyeColor color) {
        super(properties.stacksTo(1));
        this.color = color;
    }

    public DyeColor color() { return color; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        BagItemState identity = stack.get(ProjectEXComponents.BAG_IDENTITY);
        if (identity == null) {
            identity = BagItemState.create(color.getName(), player.getUUID());
            stack.set(ProjectEXComponents.BAG_IDENTITY, identity);
        }
        boolean operator = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (!identity.color().equals(color.getName()) || !identity.permits(player.getUUID(), operator)) {
            return InteractionResult.FAIL;
        }
        ItemContainerContents legacy = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        BagInventorySavedData savedData = BagInventorySavedData.get(serverPlayer.level().getServer());
        NonNullList<ItemStack> shared = savedData.inventory(identity.bagId(), legacy);
        if (legacy.equals(ItemContainerContents.EMPTY) || savedData.matches(identity.bagId(), legacy)) {
            stack.remove(DataComponents.CONTAINER);
        } else {
            ProjectEX.LOGGER.error(
                "Bag {} has divergent legacy contents; preserving the inert stack component for recovery",
                identity.bagId()
            );
        }
        BagContainer contents = new BagContainer(identity, player, savedData, shared);
        serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override public Integer getScreenOpeningData(ServerPlayer player) {
                return AlchemyStorageMenu.openingData(StorageKind.ALCHEMICAL_BAG, false);
            }
            @Override public Component getDisplayName() { return stack.getHoverName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                return new AlchemyStorageMenu(id, inventory, contents, StorageKind.ALCHEMICAL_BAG);
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }

    private static final class BagContainer implements Container {
        private final BagItemState identity;
        private final Player opener;
        private final BagInventorySavedData savedData;
        private final NonNullList<ItemStack> items;

        private BagContainer(
            BagItemState identity, Player opener, BagInventorySavedData savedData,
            NonNullList<ItemStack> items
        ) {
            this.identity = identity;
            this.opener = opener;
            this.savedData = savedData;
            this.items = items;
        }

        @Override public int getContainerSize() { return SLOTS; }
        @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) setChanged();
            return removed;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = ContainerHelper.takeItem(items, slot);
            setChanged();
            return removed;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            if (!canPlaceItem(slot, stack)) return;
            items.set(slot, stack);
            if (stack.getCount() > stack.getMaxStackSize()) stack.setCount(stack.getMaxStackSize());
            setChanged();
        }
        @Override public void setChanged() {
            savedData.inventoryChanged(identity.bagId());
        }
        @Override public boolean stillValid(Player player) {
            if (player != opener || !player.isAlive()) return false;
            return List.of(player.getMainHandItem(), player.getOffhandItem()).stream().anyMatch(stack -> {
                BagItemState current = stack.get(ProjectEXComponents.BAG_IDENTITY);
                return current != null && current.bagId().equals(identity.bagId());
            });
        }
        @Override public void clearContent() { items.clear(); setChanged(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.isEmpty() || !(stack.getItem() instanceof AlchemicalBagItem);
        }
    }
}
