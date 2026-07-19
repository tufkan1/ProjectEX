package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

/** Introductory progression for the first playable content family. */
public final class ProjectEXAdvancementProvider extends FabricAdvancementProvider {
    public ProjectEXAdvancementProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generateAdvancement(
        HolderLookup.Provider registries,
        Consumer<AdvancementHolder> exporter
    ) {
        AdvancementHolder stone = Advancement.Builder.advancement()
            .display(
                ProjectEXItems.PHILOSOPHERS_STONE.item(),
                Component.translatable("advancements.projectex.philosophers_stone.title"),
                Component.translatable("advancements.projectex.philosophers_stone.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion(
                "has_philosophers_stone",
                InventoryChangeTrigger.TriggerInstance.hasItems(
                    ProjectEXItems.PHILOSOPHERS_STONE.item()
                )
            )
            .save(exporter, ProjectEX.id("philosophers_stone").toString());

        Advancement.Builder.advancement()
            .parent(stone)
            .display(
                ProjectEXBlocks.TRANSMUTATION_TABLE,
                Component.translatable("advancements.projectex.transmutation.title"),
                Component.translatable("advancements.projectex.transmutation.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion(
                "has_transmutation_table",
                InventoryChangeTrigger.TriggerInstance.hasItems(ProjectEXBlocks.TRANSMUTATION_TABLE)
            )
            .save(exporter, ProjectEX.id("transmutation").toString());
    }
}
