package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.phys.Vec3;

/** Component-based cooking fuels introduced by 26.3 Snapshot 4. */
final class FuelCompatLegacy {
    private static final Map<Item, Integer> CUSTOM_DURATIONS = new IdentityHashMap<>();
    private static final ResourceKey<NumberProvider> ALCHEMICAL = key("cooking/alchemical_coal");
    private static final ResourceKey<NumberProvider> MOBIUS = key("cooking/mobius_fuel");
    private static final ResourceKey<NumberProvider> AETERNALIS = key("cooking/aeternalis_fuel");

    private FuelCompatLegacy() {
    }

    static void register(Item alchemical, Item mobius, Item aeternalis, List<Item> expansion) {
        CUSTOM_DURATIONS.put(alchemical, 1_600);
        CUSTOM_DURATIONS.put(mobius, 6_400);
        CUSTOM_DURATIONS.put(aeternalis, 25_600);
        expansion.forEach(item -> CUSTOM_DURATIONS.put(item, 25_600));
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(alchemical, builder -> builder.set(DataComponents.COOKING_FUEL,
                new CookingFuel(ALCHEMICAL, NumberProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
            context.modify(mobius, builder -> builder.set(DataComponents.COOKING_FUEL,
                new CookingFuel(MOBIUS, NumberProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
            context.modify(aeternalis, builder -> builder.set(DataComponents.COOKING_FUEL,
                new CookingFuel(AETERNALIS, NumberProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
            context.modify(expansion, (builder, item) -> builder.set(DataComponents.COOKING_FUEL,
                new CookingFuel(AETERNALIS, NumberProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
        });
    }

    static int burnDuration(ServerLevel level, ItemStack stack, Container container) {
        Integer custom = CUSTOM_DURATIONS.get(stack.getItem());
        if (custom != null) return custom;
        Optional<NumberProvider> provider = NumberProviders.getFromItemComponent(
            level, stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime);
        return provider.map(value -> value.getInt(context(level, container))).orElse(0);
    }

    static boolean isFuel(ServerLevel level, ItemStack stack, Container container) {
        return CUSTOM_DURATIONS.containsKey(stack.getItem()) || stack.has(DataComponents.COOKING_FUEL);
    }

    private static LootContext context(ServerLevel level, Container container) {
        BlockEntity blockEntity = (BlockEntity) container;
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockEntity.getBlockPos()))
            .withParameter(LootContextParams.CONTAINER, container)
            .create(LootContextParamSets.CONTAINER_PROCESS);
        return new LootContext.Builder(params).create(Optional.empty());
    }

    private static ResourceKey<NumberProvider> key(String path) {
        return ResourceKey.create(Registries.NUMBER_PROVIDER, ProjectEX.id(path));
    }
}
