package io.github.tufkan1.projectex.content.pedestal;

import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.machine.MachineAccess;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Claimed one-slot pedestal with explicit activation, redstone gating, and bounded work. */
public final class DarkMatterPedestalBlockEntity extends BlockEntity {
    public static final int EFFECT_RANGE = 4;
    public static final int MAXIMUM_PLAYERS_PER_ACTIVATION = 16;

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private MachineAccess access = MachineAccess.UNCLAIMED;
    private MachineRedstoneMode redstoneMode = MachineRedstoneMode.IGNORED;
    private boolean active;
    private long nextActivationTick;

    public DarkMatterPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectEXBlockEntities.DARK_MATTER_PEDESTAL, pos, state);
    }

    public static void tickServer(net.minecraft.world.level.Level rawLevel, BlockPos pos, BlockState state,
                                  DarkMatterPedestalBlockEntity pedestal) {
        if (!(rawLevel instanceof ServerLevel level)) return;
        if (!pedestal.active || !pedestal.redstoneMode.enabled(level.hasNeighborSignal(pos))) return;
        ItemStack stack = pedestal.item();
        if (!(stack.getItem() instanceof PedestalEffectItem effect)) {
            pedestal.active = false;
            pedestal.changed();
            return;
        }
        if (level.getGameTime() < pedestal.nextActivationTick) return;
        effect.applyPedestalEffect(level, pos, stack, EFFECT_RANGE, MAXIMUM_PLAYERS_PER_ACTIVATION);
        pedestal.nextActivationTick = level.getGameTime() + Math.max(1, effect.pedestalIntervalTicks());
        pedestal.changed();
    }

    public void claim(UUID owner) { access = access.claim(owner); changed(); }

    public boolean canConfigure(Player player) {
        return access.permits(player.getUUID(),
            player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
    }

    public boolean insert(ItemStack held, Player actor) {
        if (!canConfigure(actor) || !item().isEmpty()
            || !(held.getItem() instanceof PedestalEffectItem)) return false;
        items.set(0, held.split(1));
        nextActivationTick = 0;
        changed();
        return true;
    }

    public ItemStack extract(Player actor) {
        if (!canConfigure(actor)) return ItemStack.EMPTY;
        return removeItem();
    }

    public ItemStack ejectOnBreak() {
        return removeItem();
    }

    private ItemStack removeItem() {
        ItemStack extracted = items.set(0, ItemStack.EMPTY);
        if (!extracted.isEmpty()) {
            active = false;
            nextActivationTick = 0;
            changed();
        }
        return extracted;
    }

    public boolean toggleActive(Player actor) {
        if (!canConfigure(actor) || !(item().getItem() instanceof PedestalEffectItem)) return false;
        active = !active;
        nextActivationTick = 0;
        changed();
        return true;
    }

    public boolean cycleRedstoneMode(Player actor) {
        if (!canConfigure(actor)) return false;
        redstoneMode = switch (redstoneMode) {
            case IGNORED -> MachineRedstoneMode.REQUIRE_SIGNAL;
            case REQUIRE_SIGNAL -> MachineRedstoneMode.REQUIRE_NO_SIGNAL;
            case REQUIRE_NO_SIGNAL -> MachineRedstoneMode.IGNORED;
        };
        changed();
        return true;
    }

    public ItemStack item() { return items.getFirst(); }
    public boolean active() { return active; }
    public MachineAccess access() { return access; }
    public MachineRedstoneMode redstoneMode() { return redstoneMode; }
    public long nextActivationTick() { return nextActivationTick; }

    public int comparatorSignal() {
        if (item().isEmpty()) return 0;
        if (!(item().getItem() instanceof PedestalEffectItem)) return 5;
        return active ? 15 : 10;
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        active = input.getBooleanOr("active", false);
        String owner = input.getStringOr("owner", "");
        try {
            access = new MachineAccess(owner.isBlank() ? Optional.empty()
                : Optional.of(UUID.fromString(owner)), input.getBooleanOr("public_access", false));
        } catch (IllegalArgumentException exception) {
            access = MachineAccess.ownedBy(new UUID(0, 0));
            active = false;
        }
        nextActivationTick = Math.max(0, input.getLongOr("next_activation_tick", 0));
        try {
            redstoneMode = MachineRedstoneMode.valueOf(input.getStringOr(
                "redstone_mode", MachineRedstoneMode.IGNORED.name()));
        } catch (IllegalArgumentException exception) {
            redstoneMode = MachineRedstoneMode.IGNORED;
        }
        if (!(item().getItem() instanceof PedestalEffectItem)) active = false;
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("owner", access.owner().map(UUID::toString).orElse(""));
        output.putBoolean("public_access", access.publicAccess());
        output.putBoolean("active", active);
        output.putLong("next_activation_tick", nextActivationTick);
        output.putString("redstone_mode", redstoneMode.name());
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
}
