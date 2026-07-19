package io.github.tufkan1.projectex.internal.player;

import io.github.tufkan1.projectex.alchemy.AlchemyInventory;
import io.github.tufkan1.projectex.alchemy.AlchemyTransaction;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionTarget;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Component-safe adapter that atomically applies one transaction to the main player inventory. */
public final class MinecraftPlayerAlchemyTarget implements AlchemyTransactionTarget {
    private final ServerPlayer player;
    private final PlayerAlchemySavedData data;

    public MinecraftPlayerAlchemyTarget(ServerPlayer player) {
        this.player = java.util.Objects.requireNonNull(player, "player");
        this.data = PlayerAlchemySavedData.get(player.level().getServer());
    }

    @Override
    public UUID playerId() {
        return player.getUUID();
    }

    @Override
    public synchronized PlayerAlchemyState playerState() {
        return data.state(playerId());
    }

    @Override
    public synchronized AlchemyInventory inventory() {
        Map<EmcMatch, Integer> contents = contents(player.getInventory().getNonEquipmentItems());
        return new AlchemyInventory(total(contents), contents);
    }

    @Override
    public synchronized AlchemyInventory inventoryFor(AlchemyTransaction request) {
        List<ItemStack> slots = player.getInventory().getNonEquipmentItems();
        Map<EmcMatch, Integer> contents = contents(slots);
        int capacity = total(contents);
        if (request instanceof AlchemyTransaction.Create) {
            Optional<Item> requested = item(request.item());
            if (requested.isPresent()) {
                Item item = requested.orElseThrow();
                int max = item.getDefaultMaxStackSize();
                for (ItemStack stack : slots) {
                    if (stack.isEmpty()) {
                        capacity = Math.addExact(capacity, max);
                    } else if (isPlain(stack) && stack.getItem() == item) {
                        capacity = Math.addExact(capacity, Math.max(0, max - stack.getCount()));
                    }
                }
            }
        }
        return new AlchemyInventory(capacity, contents);
    }

    @Override
    public synchronized boolean commit(
        PlayerAlchemyState expectedPlayer,
        AlchemyInventory expectedInventory,
        PlayerAlchemyState newPlayer,
        AlchemyInventory newInventory
    ) {
        List<ItemStack> liveSlots = player.getInventory().getNonEquipmentItems();
        if (!data.state(playerId()).equals(expectedPlayer)
            || !contents(liveSlots).equals(expectedInventory.contents())) {
            return false;
        }
        Optional<Delta> delta = delta(expectedInventory.contents(), newInventory.contents());
        if (delta.isEmpty()) {
            return false;
        }
        if (delta.orElseThrow().count() == 0) {
            return data.compareAndSet(playerId(), expectedPlayer, newPlayer);
        }
        List<ItemStack> replacement = liveSlots.stream()
            .map(ItemStack::copy)
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (!apply(replacement, delta.orElseThrow())) {
            return false;
        }
        if (!data.compareAndSet(playerId(), expectedPlayer, newPlayer)) {
            return false;
        }
        for (int index = 0; index < replacement.size(); index++) {
            player.getInventory().setItem(index, replacement.get(index));
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean apply(List<ItemStack> slots, Delta delta) {
        Optional<Item> resolved = item(delta.item());
        if (resolved.isEmpty()) {
            return false;
        }
        Item item = resolved.orElseThrow();
        int remaining = Math.abs(delta.count());
        if (delta.count() < 0) {
            for (int index = 0; index < slots.size() && remaining > 0; index++) {
                ItemStack stack = slots.get(index);
                if (isPlain(stack) && stack.getItem() == item) {
                    int removed = Math.min(remaining, stack.getCount());
                    stack.shrink(removed);
                    remaining -= removed;
                }
            }
            return remaining == 0;
        }
        int max = item.getDefaultMaxStackSize();
        for (ItemStack stack : slots) {
            if (remaining == 0) {
                break;
            }
            if (isPlain(stack) && stack.getItem() == item && stack.getCount() < max) {
                int inserted = Math.min(remaining, max - stack.getCount());
                stack.grow(inserted);
                remaining -= inserted;
            }
        }
        for (int index = 0; index < slots.size() && remaining > 0; index++) {
            if (slots.get(index).isEmpty()) {
                int inserted = Math.min(remaining, max);
                slots.set(index, new ItemStack(item, inserted));
                remaining -= inserted;
            }
        }
        return remaining == 0;
    }

    private static Optional<Delta> delta(Map<EmcMatch, Integer> before, Map<EmcMatch, Integer> after) {
        TreeSet<EmcMatch> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        Delta found = null;
        for (EmcMatch key : keys) {
            int difference = after.getOrDefault(key, 0) - before.getOrDefault(key, 0);
            if (difference != 0) {
                if (found != null) {
                    return Optional.empty();
                }
                found = new Delta(key, difference);
            }
        }
        return Optional.of(found == null ? Delta.NONE : found);
    }

    private static Map<EmcMatch, Integer> contents(List<ItemStack> slots) {
        Map<EmcMatch, Integer> result = new TreeMap<>();
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && isPlain(stack)) {
                key(stack).ifPresent(key -> result.merge(EmcMatch.item(key), stack.getCount(), Math::addExact));
            }
        }
        return Map.copyOf(result);
    }

    private static boolean isPlain(ItemStack stack) {
        return stack.getComponentsPatch().isEmpty();
    }

    private static Optional<EmcKey> key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getResourceKey(stack.getItem())
            .map(resourceKey -> new EmcKey(
                resourceKey.identifier().getNamespace(), resourceKey.identifier().getPath()));
    }

    private static Optional<Item> item(EmcMatch match) {
        if (match.componentsJson() != null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(
            Identifier.fromNamespaceAndPath(match.item().namespace(), match.item().path()));
    }

    private static int total(Map<EmcMatch, Integer> contents) {
        return contents.values().stream().reduce(0, Math::addExact);
    }

    private record Delta(EmcMatch item, int count) {
        private static final Delta NONE = new Delta(null, 0);
    }
}
