package io.github.tufkan1.projectex.content.automation;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.automation.AutomationAccount;
import io.github.tufkan1.projectex.automation.AutomationAuditEvent;
import io.github.tufkan1.projectex.automation.AutomationAuthority;
import io.github.tufkan1.projectex.automation.CraftingTransactionTarget;
import io.github.tufkan1.projectex.automation.EmcAutomationTier;
import io.github.tufkan1.projectex.automation.EmcLinkRequest;
import io.github.tufkan1.projectex.automation.EmcLinkService;
import io.github.tufkan1.projectex.automation.AutomationOperation;
import io.github.tufkan1.projectex.automation.TransmutationCraftRequest;
import io.github.tufkan1.projectex.automation.TransmutationInterfaceService;
import io.github.tufkan1.projectex.automation.EmcLinkFilter;
import io.github.tufkan1.projectex.content.AutomationBlock;
import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.component.AutomationBlockState;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import io.github.tufkan1.projectex.menu.AutomationMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent identity/configuration boundary for one automation block. */
public final class AutomationBlockEntity extends BlockEntity implements MenuProvider {
    private final AutomationBlockKind kind;
    private final ExpansionMachineTier tier;
    private AutomationBlockState state = AutomationBlockState.empty();
    private EmcLinkService linkService;
    private TransmutationInterfaceService interfaceService;
    private AccountParticipant participant;

    public AutomationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ProjectEXBlockEntities.AUTOMATION, pos, blockState);
        if (!(blockState.getBlock() instanceof AutomationBlock block)) {
            throw new IllegalArgumentException("Automation block entity requires its registered block");
        }
        kind = block.kind();
        tier = block.tier();
    }

    public AutomationBlockKind kind() { return kind; }
    public ExpansionMachineTier tier() { return tier; }
    public AutomationBlockState automationState() { return state; }

    public void claim(UUID owner) {
        AutomationBlockState claimed = state.claim(owner);
        if (!claimed.equals(state)) {
            state = claimed;
            resetServices();
            changed();
        }
    }

    /** Prevents a stolen owner-bound block item from becoming an offline-account credential. */
    public void placedBy(Player placer) {
        if (state.owner().isEmpty()) {
            claim(placer.getUUID());
            return;
        }
        if (state.owner().orElseThrow().equals(placer.getUUID())
            || placer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        ProjectEX.LOGGER.warn("Reset stolen automation binding at {} from {} to placer {}",
            worldPosition, state.owner().orElseThrow(), placer.getUUID());
        state = AutomationBlockState.empty().claim(placer.getUUID());
        resetServices();
        changed();
    }

    public boolean canUse(Player player) {
        if (state.access().isEmpty() || !stillValid(player)) return false;
        boolean operator = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        return state.access().orElseThrow().permits(
            AutomationAuthority.online(player.getUUID(), operator),
            AutomationOperation.ENUMERATE_KNOWLEDGE
        );
    }

    public boolean togglePublicInsert(Player actor) {
        if (!canUse(actor)) return false;
        boolean operator = actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        try {
            var access = state.access().orElseThrow().withPublicInsert(
                !state.publicInsert(), AutomationAuthority.online(actor.getUUID(), operator)
            );
            state = state.withAccess(access);
            resetServices();
            changed();
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    public boolean setMember(UUID member, boolean enabled, Player actor) {
        if (!canConfigure(actor)) return false;
        boolean operator = actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        try {
            var access = state.access().orElseThrow().withMember(member, enabled,
                AutomationAuthority.online(actor.getUUID(), operator));
            state = state.withAccess(access);
            resetServices();
            changed();
            return true;
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean cycleFilterMode(boolean insertion, Player actor) {
        if (!canConfigure(actor)) return false;
        EmcLinkFilter current = insertion ? state.insertFilter() : state.extractFilter();
        EmcLinkFilter.Mode next = current.mode() == EmcLinkFilter.Mode.ALLOW_LIST
            ? EmcLinkFilter.Mode.DENY_LIST : EmcLinkFilter.Mode.ALLOW_LIST;
        EmcLinkFilter changed = new EmcLinkFilter(next, current.items());
        state = insertion ? state.withFilters(changed, state.extractFilter())
            : state.withFilters(state.insertFilter(), changed);
        resetServices();
        changed();
        return true;
    }

    public boolean toggleHeldFilter(boolean insertion, Player actor) {
        if (!canConfigure(actor) || actor.getMainHandItem().isEmpty()
            || !actor.getMainHandItem().getComponentsPatch().isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(actor.getMainHandItem().getItem());
        EmcKey key = new EmcKey(id.getNamespace(), id.getPath());
        EmcLinkFilter current = insertion ? state.insertFilter() : state.extractFilter();
        java.util.SortedSet<EmcKey> items = new java.util.TreeSet<>(current.items());
        if (!items.remove(key)) items.add(key);
        if (items.size() > EmcAutomationTier.of(tier).maximumFilterEntries()) return false;
        EmcLinkFilter changed = new EmcLinkFilter(current.mode(), items);
        state = insertion ? state.withFilters(changed, state.extractFilter())
            : state.withFilters(state.insertFilter(), changed);
        resetServices();
        changed();
        return true;
    }

    private boolean canConfigure(Player actor) {
        if (state.owner().isEmpty() || !stillValid(actor)) return false;
        return state.owner().orElseThrow().equals(actor.getUUID())
            || actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Sided Fabric item view: down inserts, up extracts, horizontal sides support both. */
    public Storage<ItemVariant> storage(Direction side) {
        if (!(level instanceof ServerLevel) || state.access().isEmpty()) return null;
        boolean insert = kind == AutomationBlockKind.EMC_LINK && side != Direction.UP;
        boolean extract = side != Direction.DOWN;
        return new SidedAutomationStorage(insert, extract);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state = input.read("automation", AutomationBlockState.CODEC)
            .orElse(AutomationBlockState.empty());
        resetServices();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("automation", AutomationBlockState.CODEC, state);
    }

    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        state = components.getOrDefault(ProjectEXComponents.AUTOMATION_STATE,
            AutomationBlockState.empty());
        resetServices();
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ProjectEXComponents.AUTOMATION_STATE, state);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return canUse(player) ? new AutomationMenu(containerId, inventory, this) : null;
    }

    private boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    private PlayerAlchemySavedData data() {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Automation account requires a server level");
        }
        return PlayerAlchemySavedData.get(serverLevel.getServer());
    }

    private UUID owner() {
        return state.owner().orElseThrow(() -> new IllegalStateException("Unclaimed automation block"));
    }

    private void ensureServices() {
        if (participant != null) return;
        participant = new AccountParticipant();
        AutomationAccount account = new AutomationAccount() {
            @Override public PlayerAlchemyState snapshot() { return data().state(owner()); }
            @Override public boolean compareAndSet(PlayerAlchemyState expected, PlayerAlchemyState replacement) {
                boolean committed = data().compareAndSet(owner(), expected, replacement);
                return committed;
            }
        };
        var access = state.access().orElseThrow();
        linkService = new EmcLinkService(EmcAutomationTier.of(tier), access,
            state.insertFilter(), state.extractFilter(), account, this::audit);
        interfaceService = new TransmutationInterfaceService(EmcAutomationTier.of(tier), access,
            new CraftingTransactionTarget() {
                @Override public Snapshot snapshot() {
                    return new Snapshot(data().state(owner()), data().revision(owner()));
                }

                @Override public CommitResult commit(Snapshot expected, PlayerAlchemyState replacement,
                                                     EmcKey item, int count) {
                    if (!data().compareAndSet(owner(), expected.account(), expected.revision(), replacement)) {
                        return CommitResult.CONTENTION;
                    }
                    return CommitResult.COMMITTED;
                }
            }, ProjectEX.emc()::snapshot, this::audit);
    }

    private void resetServices() {
        participant = null;
        linkService = null;
        interfaceService = null;
    }

    private void audit(AutomationAuditEvent event) {
        if (event.failure() != AutomationAuditEvent.Failure.NONE) {
            ProjectEX.LOGGER.debug("Automation request rejected block={} owner={} operation={} failure={}",
                worldPosition, state.owner().orElse(null), event.operation(), event.failure());
        }
    }

    private Optional<EmcKey> key(ItemVariant resource) {
        if (resource.isBlank() || resource.hasComponents()) return Optional.empty();
        Identifier id = BuiltInRegistries.ITEM.getKey(resource.getItem());
        return Optional.of(new EmcKey(id.getNamespace(), id.getPath()));
    }

    private Optional<EmcValue> value(ItemVariant resource) {
        return key(resource).flatMap(ProjectEX.emc()::find).filter(value -> !value.equals(EmcValue.ZERO));
    }

    private long boundedCount(long requested, EmcValue unit) {
        if (requested <= 0) return 0;
        BigInteger allowed = EmcAutomationTier.of(tier).maximumPerRequest().amount()
            .divide(unit.amount()).min(BigInteger.valueOf(requested));
        return allowed.min(BigInteger.valueOf(64)).longValue();
    }

    private final class AccountParticipant extends SnapshotParticipant<PlayerAlchemyState> {
        @Override protected PlayerAlchemyState createSnapshot() {
            return data().state(owner());
        }

        @Override protected void readSnapshot(PlayerAlchemyState snapshot) {
            data().update(owner(), ignored -> snapshot);
        }

        @Override protected void onFinalCommit() {
            changed();
        }
    }

    private final class SidedAutomationStorage implements Storage<ItemVariant> {
        private final boolean insert;
        private final boolean extract;

        private SidedAutomationStorage(boolean insert, boolean extract) {
            this.insert = insert;
            this.extract = extract;
        }

        @Override public boolean supportsInsertion() { return insert; }
        @Override public boolean supportsExtraction() { return extract; }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (!insert) return 0;
            Optional<EmcKey> item = key(resource);
            Optional<EmcValue> unit = value(resource);
            if (item.isEmpty() || unit.isEmpty()) return 0;
            long count = boundedCount(maxAmount, unit.orElseThrow());
            if (count == 0) return 0;
            ensureServices();
            participant.updateSnapshots(transaction);
            EmcValue amount = unit.orElseThrow().multiply(count);
            var result = linkService.transfer(new EmcLinkRequest(UUID.randomUUID(), level.getGameTime(),
                AutomationOperation.INSERT_EMC, amount, item), AutomationAuthority.machine());
            return result.successful() ? count : 0;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (!extract) return 0;
            Optional<EmcKey> item = key(resource);
            Optional<EmcValue> unit = value(resource);
            if (item.isEmpty() || unit.isEmpty()
                || !state.extractFilter().permits(item, EmcAutomationTier.of(tier).maximumFilterEntries())) {
                return 0;
            }
            long count = boundedCount(maxAmount, unit.orElseThrow());
            if (count == 0) return 0;
            ensureServices();
            participant.updateSnapshots(transaction);
            var result = interfaceService.craft(new TransmutationCraftRequest(
                UUID.randomUUID(), level.getGameTime(), item.orElseThrow(),
                ProjectEX.emc().snapshot().revision(), Math.toIntExact(count)
            ), AutomationAuthority.machine());
            return result.successful() ? result.crafted() : 0;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            if (!extract || state.owner().isEmpty()) return Collections.emptyIterator();
            ensureServices();
            int maximum = EmcAutomationTier.of(tier).maximumFilterEntries();
            return data().state(owner()).knowledge().stream()
                .filter(item -> state.extractFilter().permits(Optional.of(item), maximum))
                .map(AutomationBlockEntity.this::view)
                .flatMap(Optional::stream)
                .limit(maximum)
                .map(value -> (StorageView<ItemVariant>) new KnowledgeView(value))
                .iterator();
        }

        @Override public long getVersion() { return data().revision(owner()); }
    }

    private Optional<ItemVariant> view(EmcKey key) {
        Identifier id = Identifier.tryParse(key.toString());
        if (id == null) return Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemVariant::of);
    }

    private final class KnowledgeView implements StorageView<ItemVariant> {
        private final ItemVariant resource;

        private KnowledgeView(ItemVariant resource) { this.resource = resource; }

        @Override public long extract(ItemVariant requested, long amount, TransactionContext transaction) {
            return resource.equals(requested)
                ? new SidedAutomationStorage(false, true).extract(requested, amount, transaction) : 0;
        }
        @Override public boolean isResourceBlank() { return false; }
        @Override public ItemVariant getResource() { return resource; }
        @Override public long getAmount() {
            Optional<EmcValue> unit = value(resource);
            if (unit.isEmpty() || unit.orElseThrow().equals(EmcValue.ZERO)) return 0;
            BigInteger count = data().state(owner()).balance().amount().divide(unit.orElseThrow().amount());
            return count.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
        }
        @Override public long getCapacity() { return Long.MAX_VALUE; }
    }
}
