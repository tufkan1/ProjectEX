package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.storage.StorageKind;
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
    public static final ProjectEXContentRegistry.RegisteredBlock CONDENSER_MK1_FAMILY =
        storage("condenser_mk1", StorageKind.CONDENSER_MK1);
    public static final ProjectEXContentRegistry.RegisteredBlock CONDENSER_MK2_FAMILY =
        storage("condenser_mk2", StorageKind.CONDENSER_MK2);
    public static final ProjectEXContentRegistry.RegisteredBlock ALCHEMICAL_CHEST_FAMILY =
        storage("alchemical_chest", StorageKind.ALCHEMICAL_CHEST);
    public static final Block CONDENSER_MK1 = CONDENSER_MK1_FAMILY.block();
    public static final Block CONDENSER_MK2 = CONDENSER_MK2_FAMILY.block();
    public static final Block ALCHEMICAL_CHEST = ALCHEMICAL_CHEST_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock DARK_MATTER_BLOCK_FAMILY =
        matterBlock("dark_matter_block", SoundType.NETHERITE_BLOCK);
    public static final ProjectEXContentRegistry.RegisteredBlock RED_MATTER_BLOCK_FAMILY =
        matterBlock("red_matter_block", SoundType.NETHERITE_BLOCK);
    public static final Block DARK_MATTER_BLOCK = DARK_MATTER_BLOCK_FAMILY.block();
    public static final Block RED_MATTER_BLOCK = RED_MATTER_BLOCK_FAMILY.block();

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
                entries.accept(CONDENSER_MK1.asItem());
                entries.accept(CONDENSER_MK2.asItem());
                entries.accept(ALCHEMICAL_CHEST.asItem());
                entries.accept(DARK_MATTER_BLOCK.asItem());
                entries.accept(RED_MATTER_BLOCK.asItem());
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

    private static ProjectEXContentRegistry.RegisteredBlock storage(String id, StorageKind kind) {
        return ProjectEXContentRegistry.registerBlockWithItem(
            id,
            properties -> new AlchemyStorageBlock(properties, kind),
            BlockBehaviour.Properties.of().strength(4.0F, 18.0F).sound(SoundType.METAL)
        );
    }

    private static ProjectEXContentRegistry.RegisteredBlock matterBlock(String id, SoundType sound) {
        return ProjectEXContentRegistry.registerBlockWithItem(
            id, Block::new,
            BlockBehaviour.Properties.of().strength(20.0F, 1_200.0F)
                .requiresCorrectToolForDrops().sound(sound)
        );
    }
}
