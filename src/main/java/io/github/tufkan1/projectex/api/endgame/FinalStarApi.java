package io.github.tufkan1.projectex.api.endgame;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.Optional;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.world.item.ItemStack;

/** Public Fabric lookup for Final Star compatible items. */
public final class FinalStarApi {
    public static final int VERSION = 1;
    public static final ItemApiLookup<FinalStarCapability, FinalStarContext> LOOKUP =
        ItemApiLookup.get(ProjectEX.id("final_star"), FinalStarCapability.class, FinalStarContext.class);

    private FinalStarApi() { }

    public static Optional<FinalStarCapability> find(ItemStack stack, FinalStarContext context) {
        return Optional.ofNullable(LOOKUP.find(stack, context));
    }
}
