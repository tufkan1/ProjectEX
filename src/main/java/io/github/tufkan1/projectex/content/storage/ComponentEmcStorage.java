package io.github.tufkan1.projectex.content.storage;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcStorage;
import io.github.tufkan1.projectex.api.storage.EmcStorageContext;
import io.github.tufkan1.projectex.api.storage.EmcStorageOperation;
import io.github.tufkan1.projectex.api.storage.EmcTransferMode;
import io.github.tufkan1.projectex.api.storage.EmcTransferResult;
import io.github.tufkan1.projectex.content.KleinStarTier;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Mutable server adapter backed by one stack's persistent EMC component. */
public final class ComponentEmcStorage implements EmcStorage {
    private final ItemStack stack;
    private final KleinStarTier tier;
    private final EmcStorageContext context;

    public ComponentEmcStorage(
        ItemStack stack,
        KleinStarTier tier,
        EmcStorageContext context
    ) {
        this.stack = Objects.requireNonNull(stack, "stack");
        this.tier = Objects.requireNonNull(tier, "tier");
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public EmcValue stored() {
        return state().stored();
    }

    @Override
    public EmcValue capacity() {
        return tier.capacity();
    }

    @Override
    public boolean allows(EmcStorageOperation operation) {
        return !context.automation() || tier.automationPolicy().allows(operation);
    }

    @Override
    public EmcTransferResult insert(EmcValue requested, EmcTransferMode mode) {
        return transfer(requested, mode, EmcStorageOperation.INSERT);
    }

    @Override
    public EmcTransferResult extract(EmcValue requested, EmcTransferMode mode) {
        return transfer(requested, mode, EmcStorageOperation.EXTRACT);
    }

    private EmcTransferResult transfer(
        EmcValue requested,
        EmcTransferMode mode,
        EmcStorageOperation operation
    ) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(mode, "mode");
        ensureServerThread();
        EmcValue current = stored();
        if (!allows(operation) || current.compareTo(capacity()) > 0) {
            return new EmcTransferResult(
                requested,
                EmcValue.ZERO,
                requested,
                current,
                false,
                false
            );
        }

        EmcValue available = operation == EmcStorageOperation.INSERT
            ? capacity().subtract(current)
            : current;
        EmcValue moved = requested.min(available);
        EmcValue resulting = operation == EmcStorageOperation.INSERT
            ? current.add(moved)
            : current.subtract(moved);
        boolean execute = mode == EmcTransferMode.EXECUTE && !moved.equals(EmcValue.ZERO);
        if (execute) {
            stack.set(
                ProjectEXComponents.PORTABLE_EMC,
                new PortableEmcState(PortableEmcState.CURRENT_VERSION, resulting)
            );
        }
        return new EmcTransferResult(
            requested,
            moved,
            requested.subtract(moved),
            resulting,
            execute,
            true
        );
    }

    private PortableEmcState state() {
        return stack.getOrDefault(ProjectEXComponents.PORTABLE_EMC, PortableEmcState.EMPTY);
    }

    private void ensureServerThread() {
        if (context.level().isClientSide()
            || !context.level().getServer().isSameThread()) {
            throw new IllegalStateException("EMC storage mutation requires the owning server thread");
        }
    }
}
