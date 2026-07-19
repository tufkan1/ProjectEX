package io.github.tufkan1.projectex.api.fabric;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Explicit boundary between Minecraft identifiers/items and the loader-neutral EMC API. */
public final class MinecraftEmcAdapter {
    private MinecraftEmcAdapter() {
    }

    public static EmcKey key(Identifier identifier) {
        return new EmcKey(identifier.getNamespace(), identifier.getPath());
    }

    public static Optional<EmcKey> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).map(resourceKey -> key(resourceKey.identifier()));
    }

    /** Returns an item-only match; component-sensitive consumers should construct an exact {@link EmcMatch}. */
    public static Optional<EmcMatch> itemMatch(ItemStack stack) {
        return key(stack.getItem()).map(EmcMatch::item);
    }
}
