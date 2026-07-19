package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Data-pack extension points for ProjectEX content behavior. */
public final class ProjectEXTags {
    public static final TagKey<Item> COVALENCE_DUSTS = TagKey.create(
        Registries.ITEM,
        ProjectEX.id("covalence_dusts")
    );
    public static final TagKey<Item> KLEIN_STARS = TagKey.create(
        Registries.ITEM,
        ProjectEX.id("klein_stars")
    );
    public static final TagKey<Block> PHILOSOPHERS_STONE_ALLOWED = TagKey.create(
        Registries.BLOCK,
        ProjectEX.id("philosophers_stone_allowed")
    );
    public static final TagKey<Block> PHILOSOPHERS_STONE_DENIED = TagKey.create(
        Registries.BLOCK,
        ProjectEX.id("philosophers_stone_denied")
    );

    private ProjectEXTags() {
    }
}
