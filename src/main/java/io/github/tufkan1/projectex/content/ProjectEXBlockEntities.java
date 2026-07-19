package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types and Fabric sided automation adapters. */
public final class ProjectEXBlockEntities {
    private static final Set<Block> EMC_MACHINE_BLOCKS = Set.of(
        ProjectEXBlocks.COLLECTOR_MK1,
        ProjectEXBlocks.COLLECTOR_MK2,
        ProjectEXBlocks.COLLECTOR_MK3,
        ProjectEXBlocks.RELAY_MK1,
        ProjectEXBlocks.RELAY_MK2,
        ProjectEXBlocks.RELAY_MK3
    );

    public static final BlockEntityType<EmcMachineBlockEntity> EMC_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, ProjectEX.id("emc_machine")),
        new BlockEntityType<>(EmcMachineBlockEntity::new, EMC_MACHINE_BLOCKS)
    );
    public static final BlockEntityType<AlchemyStorageBlockEntity> ALCHEMY_STORAGE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, ProjectEX.id("alchemy_storage")),
        new BlockEntityType<>(AlchemyStorageBlockEntity::new, Set.of(
            ProjectEXBlocks.CONDENSER_MK1,
            ProjectEXBlocks.CONDENSER_MK2,
            ProjectEXBlocks.ALCHEMICAL_CHEST
        ))
    );

    private ProjectEXBlockEntities() {
    }

    public static void register() {
        ItemStorage.SIDED.registerForBlockEntity(
            (machine, direction) -> ContainerStorage.of(machine, direction),
            EMC_MACHINE
        );
        ItemStorage.SIDED.registerForBlockEntity(
            (storage, direction) -> ContainerStorage.of(storage, direction),
            ALCHEMY_STORAGE
        );
    }
}
