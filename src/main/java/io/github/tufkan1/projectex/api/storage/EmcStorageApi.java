package io.github.tufkan1.projectex.api.storage;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.Optional;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.world.item.ItemStack;

/** Stable Fabric item lookup for ProjectEX and third-party EMC containers. */
public final class EmcStorageApi {
    public static final int VERSION = 1;
    public static final ItemApiLookup<EmcStorage, EmcStorageContext> LOOKUP =
        ItemApiLookup.get(
            ProjectEX.id("emc_storage"),
            EmcStorage.class,
            EmcStorageContext.class
        );

    private EmcStorageApi() {
    }

    public static Optional<EmcStorage> find(ItemStack stack, EmcStorageContext context) {
        return Optional.ofNullable(LOOKUP.find(stack, context));
    }
}
