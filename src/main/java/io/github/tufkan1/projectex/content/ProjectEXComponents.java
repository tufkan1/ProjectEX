package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import io.github.tufkan1.projectex.content.component.MachineItemState;
import io.github.tufkan1.projectex.content.component.AlchemyStorageState;
import io.github.tufkan1.projectex.content.component.BagItemState;
import io.github.tufkan1.projectex.content.component.MatterToolState;
import io.github.tufkan1.projectex.content.component.AutomationBlockState;
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
    public static final DataComponentType<AlchemyStorageState> ALCHEMY_STORAGE_STATE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("alchemy_storage_state"),
        DataComponentType.<AlchemyStorageState>builder()
            .persistent(AlchemyStorageState.CODEC)
            .networkSynchronized(AlchemyStorageState.STREAM_CODEC)
            .build()
    );
    public static final DataComponentType<BagItemState> BAG_IDENTITY = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("bag_identity"),
        DataComponentType.<BagItemState>builder()
            .persistent(BagItemState.CODEC)
            .networkSynchronized(BagItemState.STREAM_CODEC)
            .build()
    );
    public static final DataComponentType<MatterToolState> MATTER_TOOL_STATE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("matter_tool_state"),
        DataComponentType.<MatterToolState>builder()
            .persistent(MatterToolState.CODEC)
            .networkSynchronized(MatterToolState.STREAM_CODEC)
            .build()
    );
    public static final DataComponentType<AutomationBlockState> AUTOMATION_STATE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        ProjectEX.id("automation_state"),
        DataComponentType.<AutomationBlockState>builder()
            .persistent(AutomationBlockState.CODEC)
            .networkSynchronized(AutomationBlockState.STREAM_CODEC)
            .build()
    );

    private ProjectEXComponents() {
    }

    public static void register() {
        // Class loading performs registration before items reference the component.
    }
}
