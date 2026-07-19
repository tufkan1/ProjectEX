package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import io.github.tufkan1.projectex.content.component.MachineItemState;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Persistent and network-synchronized ProjectEX data components. */
public final class ProjectEXComponents {
    public static final DataComponentType<ActiveItemState> ACTIVE_ITEM_STATE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("active_item_state"),
        DataComponentType.<ActiveItemState>builder()
            .persistent(ActiveItemState.CODEC)
            .networkSynchronized(ActiveItemState.STREAM_CODEC)
            .build()
    );
    public static final DataComponentType<PortableEmcState> PORTABLE_EMC = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("portable_emc"),
        DataComponentType.<PortableEmcState>builder()
            .persistent(PortableEmcState.CODEC)
            .networkSynchronized(PortableEmcState.STREAM_CODEC)
            .build()
    );
    public static final DataComponentType<MachineItemState> MACHINE_STATE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("machine_state"),
        DataComponentType.<MachineItemState>builder()
            .persistent(MachineItemState.CODEC)
            .networkSynchronized(MachineItemState.STREAM_CODEC)
            .build()
    );

    private ProjectEXComponents() {
    }

    public static void register() {
        // Class loading performs registration before items reference the component.
    }
}
