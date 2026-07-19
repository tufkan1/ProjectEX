package io.github.tufkan1.projectex.content.storage;

import io.github.tufkan1.projectex.api.storage.EmcStorageApi;
import io.github.tufkan1.projectex.content.KleinStarItem;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import net.minecraft.world.level.ItemLike;

/** Registers ProjectEX item providers with the public Fabric lookup. */
public final class ProjectEXEmcStorage {
    private ProjectEXEmcStorage() {
    }

    public static void register() {
        ItemLike[] stars = ProjectEXItems.kleinStars().stream()
            .map(entry -> (ItemLike) entry.item())
            .toArray(ItemLike[]::new);
        EmcStorageApi.LOOKUP.registerForItems(
            (stack, context) -> {
                if (context == null || !(stack.getItem() instanceof KleinStarItem star)) {
                    return null;
                }
                return new ComponentEmcStorage(stack, star.tier(), context);
            },
            stars
        );
    }
}
