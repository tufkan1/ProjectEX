package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcStorageApi;
import io.github.tufkan1.projectex.api.storage.EmcStorageContext;
import io.github.tufkan1.projectex.api.storage.EmcTransferMode;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.alchemy.WorldTransmutationService;
import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import io.github.tufkan1.projectex.content.recipe.KleinStarUpgradeRecipe;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import java.lang.reflect.Method;
import java.util.List;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

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

    @GameTest
    public void kleinStarLookupTransactionsRejectOverflowWithoutPrediction(GameTestHelper helper) {
        ItemStack star = new ItemStack(ProjectEXItems.KLEIN_STAR_EIN.item());
        var storage = EmcStorageApi.find(
            star,
            EmcStorageContext.automation(helper.getLevel())
        ).orElseThrow();

        var simulated = storage.insert(EmcValue.of(60_000), EmcTransferMode.SIMULATE);
        helper.assertTrue(simulated.transferred().equals(EmcValue.of(50_000)),
            "Simulation did not clamp to capacity");
        helper.assertTrue(simulated.remainder().equals(EmcValue.of(10_000)),
            "Simulation lost overflow remainder");
        helper.assertTrue(storage.stored().equals(EmcValue.ZERO),
            "Simulation mutated the authoritative component");

        var inserted = storage.insert(EmcValue.of(60_000), EmcTransferMode.EXECUTE);
        helper.assertTrue(inserted.resultingStored().equals(EmcValue.of(50_000)),
            "Execute did not fill the star exactly");
        helper.assertTrue(inserted.remainder().equals(EmcValue.of(10_000)),
            "Execute did not reject overflow");
        var extracted = storage.extract(EmcValue.of(12_345), EmcTransferMode.EXECUTE);
        helper.assertTrue(extracted.resultingStored().equals(EmcValue.of(37_655)),
            "Extract accounting was not exact");
        helper.succeed();
    }

    @GameTest
    public void kleinStarUpgradeCopyAndDropPreserveExactEmc(GameTestHelper helper) {
        ItemStack first = star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1_111);
        ItemStack second = star(ProjectEXItems.KLEIN_STAR_EIN.item(), 2_222);
        ItemStack third = star(ProjectEXItems.KLEIN_STAR_EIN.item(), 3_333);
        ItemStack fourth = star(ProjectEXItems.KLEIN_STAR_EIN.item(), 4_444);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
            first,
            second,
            third,
            fourth,
            new ItemStack(ProjectEXItems.AETERNALIS_FUEL.item()),
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
        ));
        KleinStarUpgradeRecipe recipe = new KleinStarUpgradeRecipe();

        helper.assertTrue(recipe.matches(input, helper.getLevel()),
            "Valid equal-tier upgrade did not match");
        ItemStack upgraded = recipe.assemble(input);
        helper.assertTrue(upgraded.is(ProjectEXItems.KLEIN_STAR_ZWEI.item()),
            "Upgrade produced the wrong tier");
        helper.assertTrue(stored(upgraded).equals(EmcValue.of(11_110)),
            "Upgrade did not preserve the exact input sum");
        helper.assertTrue(stored(upgraded.copy()).equals(EmcValue.of(11_110)),
            "ItemStack copy lost portable EMC");

        ItemEntity dropped = new ItemEntity(
            helper.getLevel(),
            0.5,
            1.0,
            0.5,
            upgraded.copy()
        );
        helper.getLevel().addFreshEntity(dropped);
        helper.assertTrue(stored(dropped.getItem()).equals(EmcValue.of(11_110)),
            "Dropped/death-lifecycle stack lost portable EMC");
        helper.succeed();
    }

    @GameTest
    public void kleinStarUpgradeRejectsCorruptOverflowAndMixedTiers(GameTestHelper helper) {
        KleinStarUpgradeRecipe recipe = new KleinStarUpgradeRecipe();
        CraftingInput overflow = CraftingInput.of(3, 3, List.of(
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 50_001),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 50_001),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 50_001),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 50_001),
            new ItemStack(ProjectEXItems.AETERNALIS_FUEL.item()),
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
        ));
        CraftingInput mixed = CraftingInput.of(3, 3, List.of(
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            star(ProjectEXItems.KLEIN_STAR_ZWEI.item(), 1),
            new ItemStack(ProjectEXItems.AETERNALIS_FUEL.item()),
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
        ));

        helper.assertTrue(!recipe.matches(overflow, helper.getLevel()),
            "Overflowing upgrade was accepted");
        helper.assertTrue(recipe.assemble(overflow).isEmpty(),
            "Overflowing upgrade produced a duplicating output");
        helper.assertTrue(!recipe.matches(mixed, helper.getLevel()),
            "Mixed-tier upgrade was accepted");
        helper.succeed();
    }

    @GameTest
    public void kleinStarSurvivesPlayerDeathDropExactly(GameTestHelper helper) {
        BlockPos relative = new BlockPos(10, 1, 0);
        BlockPos absolute = helper.absolutePos(relative);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        player.getInventory().add(star(ProjectEXItems.KLEIN_STAR_DREI.item(), 765_432));

        boolean previousKeepInventory = helper.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY);
        helper.getLevel().getGameRules().set(
            GameRules.KEEP_INVENTORY,
            false,
            helper.getLevel().getServer()
        );
        player.die(helper.getLevel().damageSources().genericKill());
        helper.getLevel().getGameRules().set(
            GameRules.KEEP_INVENTORY,
            previousKeepInventory,
            helper.getLevel().getServer()
        );

        ItemEntity dropped = helper.getEntities(EntityTypes.ITEM, relative, 5.0).stream()
            .filter(entity -> entity.getItem().is(ProjectEXItems.KLEIN_STAR_DREI.item()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Death did not drop the stored Klein Star"));
        helper.assertTrue(stored(dropped.getItem()).equals(EmcValue.of(765_432)),
            "Player death changed the Klein Star EMC component");
        helper.succeed();
    }

    private static ItemStack star(net.minecraft.world.item.Item item, long stored) {
        ItemStack stack = new ItemStack(item);
        stack.set(
            ProjectEXComponents.PORTABLE_EMC,
            new PortableEmcState(PortableEmcState.CURRENT_VERSION, EmcValue.of(stored))
        );
        return stack;
    }

    private static EmcValue stored(ItemStack stack) {
        return stack.getOrDefault(ProjectEXComponents.PORTABLE_EMC, PortableEmcState.EMPTY)
            .stored();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method)
        throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
