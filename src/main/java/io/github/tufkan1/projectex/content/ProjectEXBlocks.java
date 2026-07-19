package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.machine.MachineTier;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Common registration for ProjectEX blocks and their block items. */
public final class ProjectEXBlocks {
    public static final ProjectEXContentRegistry.RegisteredBlock TRANSMUTATION_TABLE_FAMILY =
        ProjectEXContentRegistry.registerBlockWithItem(
        "transmutation_table",
        TransmutationTableBlock::new,
        BlockBehaviour.Properties.of().strength(5.0F, 1_200.0F)
            .requiresCorrectToolForDrops().sound(SoundType.STONE)
    );
    public static final Block TRANSMUTATION_TABLE = TRANSMUTATION_TABLE_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock COLLECTOR_MK1_FAMILY =
        machine("collector_mk1", MachineTier.COLLECTOR_MK1);
    public static final ProjectEXContentRegistry.RegisteredBlock COLLECTOR_MK2_FAMILY =
        machine("collector_mk2", MachineTier.COLLECTOR_MK2);
    public static final ProjectEXContentRegistry.RegisteredBlock COLLECTOR_MK3_FAMILY =
        machine("collector_mk3", MachineTier.COLLECTOR_MK3);
    public static final ProjectEXContentRegistry.RegisteredBlock RELAY_MK1_FAMILY =
        machine("relay_mk1", MachineTier.RELAY_MK1);
    public static final ProjectEXContentRegistry.RegisteredBlock RELAY_MK2_FAMILY =
        machine("relay_mk2", MachineTier.RELAY_MK2);
    public static final ProjectEXContentRegistry.RegisteredBlock RELAY_MK3_FAMILY =
        machine("relay_mk3", MachineTier.RELAY_MK3);
    public static final Block COLLECTOR_MK1 = COLLECTOR_MK1_FAMILY.block();
    public static final Block COLLECTOR_MK2 = COLLECTOR_MK2_FAMILY.block();
    public static final Block COLLECTOR_MK3 = COLLECTOR_MK3_FAMILY.block();
    public static final Block RELAY_MK1 = RELAY_MK1_FAMILY.block();
    public static final Block RELAY_MK2 = RELAY_MK2_FAMILY.block();
    public static final Block RELAY_MK3 = RELAY_MK3_FAMILY.block();

    private ProjectEXBlocks() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(entries -> {
                entries.accept(TRANSMUTATION_TABLE.asItem());
                entries.accept(COLLECTOR_MK1.asItem());
                entries.accept(COLLECTOR_MK2.asItem());
                entries.accept(COLLECTOR_MK3.asItem());
                entries.accept(RELAY_MK1.asItem());
                entries.accept(RELAY_MK2.asItem());
                entries.accept(RELAY_MK3.asItem());
            });
    }

    private static ProjectEXContentRegistry.RegisteredBlock machine(
        String id,
        MachineTier tier
    ) {
        return ProjectEXContentRegistry.registerBlockWithItem(
            id,
            properties -> new EmcMachineBlock(properties, tier),
            BlockBehaviour.Properties.of().strength(3.5F, 12.0F).sound(SoundType.METAL)
        );
    }
}
