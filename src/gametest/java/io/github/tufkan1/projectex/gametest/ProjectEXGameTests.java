package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
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

/** Runtime registration, resource reload, and physical menu smoke tests. */
public final class ProjectEXGameTests implements CustomTestMethodInvoker {
    @GameTest
    @SuppressWarnings("removal")
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

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method)
        throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
