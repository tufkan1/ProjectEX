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
    public static final TagKey<Item> FUELS = TagKey.create(Registries.ITEM, ProjectEX.id("fuels"));
    public static final TagKey<Item> MATTERS = TagKey.create(Registries.ITEM, ProjectEX.id("matters"));
    public static final TagKey<Block> PHILOSOPHERS_STONE_ALLOWED = TagKey.create(
        Registries.BLOCK,
        ProjectEX.id("philosophers_stone_allowed")
    );
    public static final TagKey<Block> PHILOSOPHERS_STONE_DENIED = TagKey.create(
        Registries.BLOCK,
        ProjectEX.id("philosophers_stone_denied")
    );
    public static final TagKey<Block> DIVINING_ROD_ALLOWED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("divining_rod_allowed")
    );
    public static final TagKey<Block> DIVINING_ROD_DENIED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("divining_rod_denied")
    );
    public static final TagKey<Block> ELEMENTAL_AMULET_ALLOWED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("elemental_amulet_allowed")
    );
    public static final TagKey<Block> ELEMENTAL_AMULET_DENIED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("elemental_amulet_denied")
    );
    public static final TagKey<Block> DESTRUCTIVE_CATALYST_ALLOWED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("destructive_catalyst_allowed")
    );
    public static final TagKey<Block> DESTRUCTIVE_CATALYST_DENIED = TagKey.create(
        Registries.BLOCK, ProjectEX.id("destructive_catalyst_denied")
    );
    public static final TagKey<Block> INCORRECT_FOR_DARK_MATTER_TOOL = TagKey.create(
        Registries.BLOCK, ProjectEX.id("incorrect_for_dark_matter_tool")
    );
    public static final TagKey<Block> INCORRECT_FOR_RED_MATTER_TOOL = TagKey.create(
        Registries.BLOCK, ProjectEX.id("incorrect_for_red_matter_tool")
    );
    public static final TagKey<Item> DARK_MATTER_REPAIR = TagKey.create(
        Registries.ITEM, ProjectEX.id("dark_matter_repair")
    );
    public static final TagKey<Item> RED_MATTER_REPAIR = TagKey.create(
        Registries.ITEM, ProjectEX.id("red_matter_repair")
    );

    private ProjectEXTags() {
    }
}
