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
    }

    private static ResourceKey<Item> key(
        ProjectEXContentRegistry.RegisteredItem<? extends Item> item
    ) {
        return ResourceKey.create(Registries.ITEM, item.id());
    }
}
