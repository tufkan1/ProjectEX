package io.github.tufkan1.projectex.api.teleport;

import io.github.tufkan1.projectex.teleport.AlchemicalBookTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Immutable claim/protection query issued before EMC debit or teleportation. */
public record AlchemicalTeleportContext(
    ServerPlayer player, ItemStack book, AlchemicalBookTier tier,
    ServerLevel sourceLevel, BlockPos source, ServerLevel destinationLevel, BlockPos destination
) { }
