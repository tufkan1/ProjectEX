package io.github.tufkan1.projectex.internal.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.AlchemicalBagItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-owned UUID-keyed bag inventories; duplicate stacks are mirrors, never duplicate stores. */
public final class BagInventorySavedData extends SavedData {
    private static final Codec<BagInventorySavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, ItemContainerContents.CODEC).fieldOf("bags")
                .forGetter(BagInventorySavedData::encoded)
        ).apply(instance, BagInventorySavedData::new)
    );
    private static final SavedDataType<BagInventorySavedData> TYPE = new SavedDataType<>(
        ProjectEX.id("alchemical_bags"), BagInventorySavedData::new, CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, NonNullList<ItemStack>> bags = new HashMap<>();

    public BagInventorySavedData() {
    }

    private BagInventorySavedData(Map<String, ItemContainerContents> encoded) {
        encoded.forEach((id, contents) -> {
            try {
                NonNullList<ItemStack> items = NonNullList.withSize(AlchemicalBagItem.SLOTS, ItemStack.EMPTY);
                contents.copyInto(items);
                bags.put(UUID.fromString(id), items);
            } catch (IllegalArgumentException exception) {
                ProjectEX.LOGGER.error("Discarded invalid alchemical bag id {}", id, exception);
            }
        });
    }

    public static BagInventorySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Returns the single shared inventory for an id, importing legacy stack contents once. */
    public synchronized NonNullList<ItemStack> inventory(UUID id, ItemContainerContents legacy) {
        return bags.computeIfAbsent(id, ignored -> {
            NonNullList<ItemStack> created = NonNullList.withSize(AlchemicalBagItem.SLOTS, ItemStack.EMPTY);
            legacy.copyInto(created);
            setDirty();
            return created;
        });
    }

    public synchronized boolean matches(UUID id, ItemContainerContents contents) {
        NonNullList<ItemStack> stored = bags.get(id);
        return stored != null && ItemContainerContents.fromItems(stored).equals(contents);
    }

    public synchronized void inventoryChanged(UUID id) {
        if (!bags.containsKey(id)) throw new IllegalStateException("Unknown alchemical bag inventory");
        setDirty();
    }

    private synchronized Map<String, ItemContainerContents> encoded() {
        Map<String, ItemContainerContents> encoded = new java.util.TreeMap<>();
        bags.forEach((id, items) -> encoded.put(id.toString(), ItemContainerContents.fromItems(items)));
        return encoded;
    }
}
