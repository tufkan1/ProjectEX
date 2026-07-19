package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.alchemy.WorldTransmutationService;
import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;

/** Runtime registration, resource reload, and physical menu smoke tests. */
@SuppressWarnings("removal")
public final class ProjectEXGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void tableOpensServerOwnedMenu(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 0, 0);
        helper.setBlock(relative, ProjectEXBlocks.TRANSMUTATION_TABLE);
        helper.assertBlockPresent(ProjectEXBlocks.TRANSMUTATION_TABLE, relative);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(relative);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        helper.getBlockState(relative).useWithoutItem(
            helper.getLevel(),
            player,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        helper.assertTrue(player.containerMenu instanceof TransmutationMenu,
            "Transmutation Table did not open its server menu");
        player.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void bundledEmcDataLoadsAtRuntime(GameTestHelper helper) {
        helper.assertValueEqual(
            ProjectEX.emc().find(EmcKey.parse("minecraft:diamond")).orElseThrow(),
            EmcValue.of(8192),
            "Bundled diamond EMC"
        );
        helper.assertTrue(ProjectEX.emc().snapshot().size() >= 20,
            "Bundled EMC snapshot is incomplete");
        helper.succeed();
    }

    @GameTest
    public void coreItemsAndActiveStateLoadAtRuntime(GameTestHelper helper) {
        ItemStack stone = new ItemStack(ProjectEXItems.PHILOSOPHERS_STONE.item());
        helper.assertTrue(
            stone.getOrDefault(ProjectEXComponents.ACTIVE_ITEM_STATE, ActiveItemState.DEFAULT)
                .equals(ActiveItemState.DEFAULT),
            "Philosopher's Stone did not receive its default active state"
        );
        helper.assertTrue(
            ProjectEXItems.materials().size() == 8,
            "Core material family is incomplete"
        );
        helper.succeed();
    }

    @GameTest
    public void philosophersStoneTransformsAllowedAndIgnoresUnsupportedBlocks(
        GameTestHelper helper
    ) {
        BlockPos allowed = new BlockPos(2, 0, 0);
        BlockPos unsupported = new BlockPos(4, 0, 0);
        helper.setBlock(allowed, Blocks.STONE);
        helper.setBlock(unsupported, Blocks.OBSIDIAN);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absoluteAllowed = helper.absolutePos(allowed);
        player.setPos(
            absoluteAllowed.getX() + 0.5,
            absoluteAllowed.getY() + 1.0,
            absoluteAllowed.getZ() + 0.5
        );
        ItemStack catalyst = new ItemStack(ProjectEXItems.PHILOSOPHERS_STONE.item());

        WorldTransmutationService.Result changed = WorldTransmutationService.transform(
            helper.getLevel(),
            player,
            catalyst,
            absoluteAllowed,
            Direction.UP,
            Direction.NORTH,
            ActiveItemState.DEFAULT,
            false
        );
        WorldTransmutationService.Result ignored = WorldTransmutationService.transform(
            helper.getLevel(),
            player,
            catalyst,
            helper.absolutePos(unsupported),
            Direction.UP,
            Direction.NORTH,
            ActiveItemState.DEFAULT,
            false
        );

        helper.assertTrue(changed.status() == WorldTransmutationService.Status.CHANGED,
            "Allowed stone transformation failed: " + changed.status());
        helper.assertBlockPresent(Blocks.COBBLESTONE, allowed);
        helper.assertTrue(ignored.status() == WorldTransmutationService.Status.UNSUPPORTED,
            "Unsupported block was not ignored");
        helper.assertBlockPresent(Blocks.OBSIDIAN, unsupported);
        helper.succeed();
    }

    @GameTest
    public void protectionHookVetoesCompleteTransformationPlan(GameTestHelper helper) {
        BlockPos relative = new BlockPos(6, 0, 0);
        helper.setBlock(relative, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(relative);
        WorldTransmutationProtection.EVENT.register(
            context -> !context.position().equals(absolute)
        );
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);

        WorldTransmutationService.Result result = WorldTransmutationService.transform(
            helper.getLevel(),
            player,
            new ItemStack(ProjectEXItems.PHILOSOPHERS_STONE.item()),
            absolute,
            Direction.UP,
            Direction.NORTH,
            ActiveItemState.DEFAULT,
            false
        );

        helper.assertTrue(result.status() == WorldTransmutationService.Status.DENIED,
            "Protection callback did not veto the plan");
        helper.assertBlockPresent(Blocks.STONE, relative);
        helper.succeed();
    }

    @GameTest
    public void productionItemUseTransformsAndStartsServerCooldown(GameTestHelper helper) {
        BlockPos relative = new BlockPos(8, 0, 0);
        helper.setBlock(relative, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(relative);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        ItemStack catalyst = new ItemStack(ProjectEXItems.PHILOSOPHERS_STONE.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, catalyst);
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(absolute),
            Direction.UP,
            absolute,
            false
        );

        ProjectEXItems.PHILOSOPHERS_STONE.item().useOn(
            new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
        );

        helper.assertBlockPresent(Blocks.COBBLESTONE, relative);
        helper.assertTrue(player.getCooldowns().isOnCooldown(catalyst),
            "Successful production item use did not start a server cooldown");
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method)
        throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
