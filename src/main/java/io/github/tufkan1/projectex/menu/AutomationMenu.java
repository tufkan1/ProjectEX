package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import io.github.tufkan1.projectex.content.automation.AutomationBlockKind;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Read-only account status plus owner-authorized access/filter controls. */
public final class AutomationMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 7;
    private final ContainerData data;
    private final AutomationBlockEntity automation;

    public AutomationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(DATA_COUNT));
    }

    public AutomationMenu(int containerId, Inventory inventory, Integer openingData) {
        this(containerId, inventory, null, fixedData(openingData));
    }

    private static ContainerData fixedData(int openingData) {
        SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
        data.set(0, openingData >>> 8);
        data.set(1, openingData & 0xFF);
        return data;
    }

    public AutomationMenu(int containerId, Inventory inventory, AutomationBlockEntity automation) {
        this(containerId, inventory, automation, new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> automation.tier().ordinal();
                    case 1 -> automation.kind().ordinal();
                    case 2 -> automation.automationState().publicInsert() ? 1 : 0;
                    case 3 -> automation.automationState().insertFilter().mode().ordinal();
                    case 4 -> automation.automationState().extractFilter().mode().ordinal();
                    case 5 -> automation.automationState().insertFilter().items().size();
                    case 6 -> automation.automationState().extractFilter().items().size();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        });
    }

    private AutomationMenu(int containerId, Inventory inventory, AutomationBlockEntity automation,
                           ContainerData data) {
        super(ProjectEXMenus.AUTOMATION, containerId);
        this.automation = automation;
        this.data = data;
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public ExpansionMachineTier tier() {
        int ordinal = data.get(0);
        return ordinal >= 0 && ordinal < ExpansionMachineTier.values().length
            ? ExpansionMachineTier.values()[ordinal] : ExpansionMachineTier.BASIC;
    }
    public AutomationBlockKind kind() {
        int ordinal = data.get(1);
        return ordinal >= 0 && ordinal < AutomationBlockKind.values().length
            ? AutomationBlockKind.values()[ordinal] : AutomationBlockKind.EMC_LINK;
    }
    public boolean publicInsert() { return data.get(2) != 0; }
    public int insertMode() { return data.get(3); }
    public int extractMode() { return data.get(4); }
    public int insertEntries() { return data.get(5); }
    public int extractEntries() { return data.get(6); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (automation == null || !automation.canUse(player)) return false;
        return switch (id) {
            case 0 -> automation.togglePublicInsert(player);
            case 1 -> automation.cycleFilterMode(true, player);
            case 2 -> automation.cycleFilterMode(false, player);
            case 3 -> automation.toggleHeldFilter(true, player);
            case 4 -> automation.toggleHeldFilter(false, player);
            default -> false;
        };
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) {
        return automation == null || automation.canUse(player);
    }
}
