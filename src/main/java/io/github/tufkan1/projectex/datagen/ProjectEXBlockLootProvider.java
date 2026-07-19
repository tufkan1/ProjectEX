package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Block loot tables for ProjectEX content. */
public final class ProjectEXBlockLootProvider extends FabricBlockLootSubProvider {
    public ProjectEXBlockLootProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(ProjectEXBlocks.TRANSMUTATION_TABLE);
        machine(ProjectEXBlocks.COLLECTOR_MK1);
        machine(ProjectEXBlocks.COLLECTOR_MK2);
        machine(ProjectEXBlocks.COLLECTOR_MK3);
        machine(ProjectEXBlocks.RELAY_MK1);
        machine(ProjectEXBlocks.RELAY_MK2);
        machine(ProjectEXBlocks.RELAY_MK3);
        ProjectEXBlocks.EXPANSION_COLLECTORS.values().forEach(entry -> machine(entry.block()));
        ProjectEXBlocks.EXPANSION_RELAYS.values().forEach(entry -> machine(entry.block()));
        ProjectEXBlocks.POWER_FLOWERS.values().forEach(entry -> machine(entry.block()));
        dropSelf(ProjectEXBlocks.COMPACT_SUN);
        storage(ProjectEXBlocks.CONDENSER_MK1);
        storage(ProjectEXBlocks.CONDENSER_MK2);
        storage(ProjectEXBlocks.ALCHEMICAL_CHEST);
        dropSelf(ProjectEXBlocks.DARK_MATTER_BLOCK);
        dropSelf(ProjectEXBlocks.RED_MATTER_BLOCK);
        container(ProjectEXBlocks.DARK_MATTER_FURNACE);
        container(ProjectEXBlocks.RED_MATTER_FURNACE);
        ProjectEXBlocks.EMC_LINKS.values().forEach(entry -> automation(entry.block()));
        automation(ProjectEXBlocks.TRANSMUTATION_INTERFACE);
    }

    private void machine(net.minecraft.world.level.block.Block block) {
        add(block, createSingleItemTable(block).apply(
            CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                .include(ProjectEXComponents.MACHINE_STATE)
                .include(DataComponents.CONTAINER)
        ));
    }

    private void storage(net.minecraft.world.level.block.Block block) {
        add(block, createSingleItemTable(block).apply(
            CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                .include(ProjectEXComponents.ALCHEMY_STORAGE_STATE)
                .include(DataComponents.CONTAINER)
        ));
    }

    private void container(net.minecraft.world.level.block.Block block) {
        add(block, createSingleItemTable(block).apply(
            CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                .include(DataComponents.CONTAINER)
        ));
    }

    private void automation(net.minecraft.world.level.block.Block block) {
        add(block, createSingleItemTable(block).apply(
            CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                .include(ProjectEXComponents.AUTOMATION_STATE)
        ));
    }
}
