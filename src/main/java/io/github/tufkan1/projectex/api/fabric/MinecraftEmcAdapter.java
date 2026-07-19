package io.github.tufkan1.projectex.api.fabric;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.RegistryOps;
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

    /** Serializes a stack patch exactly as the canonical `components` object in EMC data files. */
    public static Optional<EmcMatch> exactMatch(ItemStack stack, HolderLookup.Provider registries) {
        Optional<EmcKey> key = key(stack.getItem());
        if (key.isEmpty()) return Optional.empty();
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return Optional.of(EmcMatch.item(key.orElseThrow()));
        return DataComponentPatch.CODEC.encodeStart(
            RegistryOps.create(JsonOps.INSTANCE, registries), patch
        ).result().map(json -> new EmcMatch(key.orElseThrow(), new Gson().toJson(canonicalize(json))));
    }

    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject sorted = new JsonObject();
            TreeMap<String, JsonElement> fields = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((name, value) -> sorted.add(name, canonicalize(value)));
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            element.getAsJsonArray().forEach(value -> array.add(canonicalize(value)));
            return array;
        }
        return element.deepCopy();
    }
}
