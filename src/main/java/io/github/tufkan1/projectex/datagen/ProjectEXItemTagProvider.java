package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.content.ProjectEXContentRegistry;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.ProjectEXTags;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** ProjectEX material families exposed to recipes and integrations. */
public final class ProjectEXItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public ProjectEXItemTagProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        builder(ProjectEXTags.COVALENCE_DUSTS).add(
            key(ProjectEXItems.LOW_COVALENCE_DUST),
            key(ProjectEXItems.MEDIUM_COVALENCE_DUST),
            key(ProjectEXItems.HIGH_COVALENCE_DUST)
        );
        builder(ProjectEXTags.KLEIN_STARS).addAll(
            ProjectEXItems.kleinStars().stream().map(ProjectEXItemTagProvider::key).toList()
        );
        builder(ProjectEXTags.FUELS).add(
            key(ProjectEXItems.ALCHEMICAL_COAL), key(ProjectEXItems.MOBIUS_FUEL),
            key(ProjectEXItems.AETERNALIS_FUEL)
        ).addAll(ProjectEXItems.EXPANSION_FUELS.stream().map(ProjectEXItemTagProvider::key).toList());
        builder(ProjectEXTags.MATTERS).add(
            key(ProjectEXItems.DARK_MATTER), key(ProjectEXItems.RED_MATTER),
            key(ProjectEXItems.FADING_MATTER)
        ).addAll(ProjectEXItems.EXPANSION_MATTERS.stream().map(ProjectEXItemTagProvider::key).toList());
        builder(ProjectEXTags.DARK_MATTER_REPAIR).add(key(ProjectEXItems.DARK_MATTER));
        builder(ProjectEXTags.RED_MATTER_REPAIR).add(key(ProjectEXItems.RED_MATTER));
    }

    private static ResourceKey<Item> key(
        ProjectEXContentRegistry.RegisteredItem<? extends Item> item
    ) {
        return ResourceKey.create(Registries.ITEM, item.id());
    }
}
