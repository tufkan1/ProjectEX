package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.storage.StorageKind;
import io.github.tufkan1.projectex.content.automation.AutomationBlockKind;
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
    public static final java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        EXPANSION_COLLECTORS = expansionMachines(MachineTier.MachineType.COLLECTOR);
    public static final java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        EXPANSION_RELAYS = expansionMachines(MachineTier.MachineType.RELAY);
    public static final java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        POWER_FLOWERS = expansionMachines(MachineTier.MachineType.POWER_FLOWER);
    public static final ProjectEXContentRegistry.RegisteredBlock COMPACT_SUN_FAMILY =
        ProjectEXContentRegistry.registerBlockWithItem(
            "compact_sun",
            Block::new,
            BlockBehaviour.Properties.of().strength(50.0F, 1_200.0F)
                .requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(state -> 15)
        );
    public static final Block COMPACT_SUN = COMPACT_SUN_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock CONDENSER_MK1_FAMILY =
        storage("condenser_mk1", StorageKind.CONDENSER_MK1);
    public static final ProjectEXContentRegistry.RegisteredBlock CONDENSER_MK2_FAMILY =
        storage("condenser_mk2", StorageKind.CONDENSER_MK2);
    public static final ProjectEXContentRegistry.RegisteredBlock CONDENSER_MK3_FAMILY =
        storage("condenser_mk3", StorageKind.CONDENSER_MK3);
    public static final ProjectEXContentRegistry.RegisteredBlock ALCHEMICAL_CHEST_FAMILY =
        storage("alchemical_chest", StorageKind.ALCHEMICAL_CHEST);
    public static final ProjectEXContentRegistry.RegisteredBlock ADVANCED_ALCHEMICAL_CHEST_FAMILY =
        storage("advanced_alchemical_chest", StorageKind.ADVANCED_ALCHEMICAL_CHEST);
    public static final Block CONDENSER_MK1 = CONDENSER_MK1_FAMILY.block();
    public static final Block CONDENSER_MK2 = CONDENSER_MK2_FAMILY.block();
    public static final Block CONDENSER_MK3 = CONDENSER_MK3_FAMILY.block();
    public static final Block ALCHEMICAL_CHEST = ALCHEMICAL_CHEST_FAMILY.block();
    public static final Block ADVANCED_ALCHEMICAL_CHEST = ADVANCED_ALCHEMICAL_CHEST_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock DARK_MATTER_BLOCK_FAMILY =
        matterBlock("dark_matter_block", SoundType.NETHERITE_BLOCK);
    public static final ProjectEXContentRegistry.RegisteredBlock RED_MATTER_BLOCK_FAMILY =
        matterBlock("red_matter_block", SoundType.NETHERITE_BLOCK);
    public static final Block DARK_MATTER_BLOCK = DARK_MATTER_BLOCK_FAMILY.block();
    public static final Block RED_MATTER_BLOCK = RED_MATTER_BLOCK_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock DARK_MATTER_FURNACE_FAMILY =
        matterFurnace("dark_matter_furnace", io.github.tufkan1.projectex.matter.MatterTier.DARK);
    public static final ProjectEXContentRegistry.RegisteredBlock RED_MATTER_FURNACE_FAMILY =
        matterFurnace("red_matter_furnace", io.github.tufkan1.projectex.matter.MatterTier.RED);
    public static final Block DARK_MATTER_FURNACE = DARK_MATTER_FURNACE_FAMILY.block();
    public static final Block RED_MATTER_FURNACE = RED_MATTER_FURNACE_FAMILY.block();
    public static final java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        EMC_LINKS = automationLinks();
    public static final ProjectEXContentRegistry.RegisteredBlock TRANSMUTATION_INTERFACE_FAMILY =
        ProjectEXContentRegistry.registerBlockWithItem(
            "transmutation_interface",
            properties -> new AutomationBlock(properties, AutomationBlockKind.TRANSMUTATION_INTERFACE,
                ExpansionMachineTier.FINAL),
            BlockBehaviour.Properties.of().strength(12.0F, 1_200.0F).sound(SoundType.METAL)
        );
    public static final Block TRANSMUTATION_INTERFACE = TRANSMUTATION_INTERFACE_FAMILY.block();
    public static final ProjectEXContentRegistry.RegisteredBlock DARK_MATTER_PEDESTAL_FAMILY =
        ProjectEXContentRegistry.registerBlockWithItem(
            "dark_matter_pedestal", DarkMatterPedestalBlock::new,
            BlockBehaviour.Properties.of().strength(20.0F, 1_200.0F)
                .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)
        );
    public static final Block DARK_MATTER_PEDESTAL = DARK_MATTER_PEDESTAL_FAMILY.block();

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
                EXPANSION_COLLECTORS.values().forEach(entry -> entries.accept(entry.item()));
                EXPANSION_RELAYS.values().forEach(entry -> entries.accept(entry.item()));
                POWER_FLOWERS.values().forEach(entry -> entries.accept(entry.item()));
                entries.accept(COMPACT_SUN.asItem());
                entries.accept(CONDENSER_MK1.asItem());
                entries.accept(CONDENSER_MK2.asItem());
                entries.accept(CONDENSER_MK3.asItem());
                entries.accept(ALCHEMICAL_CHEST.asItem());
                entries.accept(ADVANCED_ALCHEMICAL_CHEST.asItem());
                entries.accept(DARK_MATTER_BLOCK.asItem());
                entries.accept(RED_MATTER_BLOCK.asItem());
                entries.accept(DARK_MATTER_FURNACE.asItem());
                entries.accept(RED_MATTER_FURNACE.asItem());
                EMC_LINKS.values().forEach(entry -> entries.accept(entry.item()));
                entries.accept(TRANSMUTATION_INTERFACE.asItem());
                entries.accept(DARK_MATTER_PEDESTAL.asItem());
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

    private static java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        expansionMachines(MachineTier.MachineType type) {
        java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock> machines =
            new java.util.LinkedHashMap<>();
        for (ExpansionMachineTier expansionTier : ExpansionMachineTier.values()) {
            if (type != MachineTier.MachineType.POWER_FLOWER
                && expansionTier.ordinal() < ExpansionMachineTier.MAGENTA.ordinal()) {
                continue;
            }
            String suffix = switch (type) {
                case COLLECTOR -> "collector";
                case RELAY -> "relay";
                case POWER_FLOWER -> "power_flower";
            };
            machines.put(expansionTier, machine(
                expansionTier.id() + "_" + suffix,
                MachineTier.expansion(type, expansionTier)
            ));
        }
        return java.util.Collections.unmodifiableMap(machines);
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

    private static ProjectEXContentRegistry.RegisteredBlock matterFurnace(
        String id, io.github.tufkan1.projectex.matter.MatterTier tier
    ) {
        return ProjectEXContentRegistry.registerBlockWithItem(
            id, properties -> new MatterFurnaceBlock(properties, tier),
            BlockBehaviour.Properties.of().strength(8.0F, 1_200.0F).sound(SoundType.METAL)
        );
    }

    private static java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock>
        automationLinks() {
        java.util.Map<ExpansionMachineTier, ProjectEXContentRegistry.RegisteredBlock> links =
            new java.util.LinkedHashMap<>();
        for (ExpansionMachineTier tier : ExpansionMachineTier.values()) {
            links.put(tier, ProjectEXContentRegistry.registerBlockWithItem(
                tier.id() + "_emc_link",
                properties -> new AutomationBlock(properties, AutomationBlockKind.EMC_LINK, tier),
                BlockBehaviour.Properties.of().strength(6.0F, 1_200.0F).sound(SoundType.METAL)
            ));
        }
        return java.util.Collections.unmodifiableMap(links);
    }
}
