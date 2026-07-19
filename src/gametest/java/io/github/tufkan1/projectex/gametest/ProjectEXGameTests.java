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
import io.github.tufkan1.projectex.content.component.MachineItemState;
import io.github.tufkan1.projectex.content.component.MatterToolState;
import io.github.tufkan1.projectex.api.matter.MatterAreaActionProtection;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import io.github.tufkan1.projectex.content.recipe.KleinStarUpgradeRecipe;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import io.github.tufkan1.projectex.content.matter.MatterFurnaceBlockEntity;
import io.github.tufkan1.projectex.content.AlchemicalBagItem;
import io.github.tufkan1.projectex.content.component.BagItemState;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.matter.MatterTierConfig;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponents;

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
        helper.assertValueEqual(MatterTierConfig.snapshot().size(), 2,
            "Bundled matter tier definition count");
        helper.assertValueEqual(MatterTierConfig.snapshot().get("red_matter").furnaceCookTicks(), 5,
            "Bundled red matter cook time");
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
        CraftingInput individualOverflow = CraftingInput.of(3, 3, List.of(
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 50_001),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            star(ProjectEXItems.KLEIN_STAR_EIN.item(), 1),
            new ItemStack(ProjectEXItems.AETERNALIS_FUEL.item()),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));

        helper.assertTrue(!recipe.matches(overflow, helper.getLevel()),
            "Overflowing upgrade was accepted");
        helper.assertTrue(recipe.assemble(overflow).isEmpty(),
            "Overflowing upgrade produced a duplicating output");
        helper.assertTrue(!recipe.matches(mixed, helper.getLevel()),
            "Mixed-tier upgrade was accepted");
        helper.assertTrue(!recipe.matches(individualOverflow, helper.getLevel()),
            "Individually overflowing source star was accepted");
        helper.succeed();
    }

    @GameTest
    public void kleinOmegaUpgradeCrossesIntoMagnumAndPreservesExactEmc(GameTestHelper helper) {
        KleinStarUpgradeRecipe recipe = new KleinStarUpgradeRecipe();
        CraftingInput input = CraftingInput.of(3, 3, List.of(
            star(ProjectEXItems.KLEIN_STAR_OMEGA.item(), 12_345_678),
            star(ProjectEXItems.KLEIN_STAR_OMEGA.item(), 23_456_789),
            star(ProjectEXItems.KLEIN_STAR_OMEGA.item(), 34_567_890),
            star(ProjectEXItems.KLEIN_STAR_OMEGA.item(), 45_678_901),
            new ItemStack(ProjectEXItems.AETERNALIS_FUEL.item()),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));

        helper.assertTrue(recipe.matches(input, helper.getLevel()),
            "Klein Omega to Magnum boundary did not match");
        ItemStack upgraded = recipe.assemble(input);
        helper.assertTrue(upgraded.is(ProjectEXItems.MAGNUM_STAR_EIN.item()),
            "Klein Omega did not upgrade into Magnum Ein");
        helper.assertTrue(stored(upgraded).equals(EmcValue.of(116_049_258)),
            "Boundary upgrade rounded or lost portable EMC");
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

    @GameTest
    public void collectorGeneratesAndRelayCycleConservesEmc(GameTestHelper helper) {
        BlockPos collectorPos = new BlockPos(0, 1, 0);
        BlockPos relayPos = new BlockPos(1, 1, 0);
        helper.setBlock(collectorPos, ProjectEXBlocks.COLLECTOR_MK1);
        helper.setBlock(relayPos, ProjectEXBlocks.RELAY_MK1);
        EmcMachineBlockEntity collector = machine(helper, collectorPos);
        EmcMachineBlockEntity relay = machine(helper, relayPos);

        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(collectorPos), collector.getBlockState(), collector
        );
        helper.assertTrue(collector.machineState().stored().equals(EmcValue.ZERO),
            "Collector did not transfer its exact generated EMC");
        helper.assertTrue(relay.machineState().stored().equals(EmcValue.of(4)),
            "Relay did not receive the MK1 collector rate");

        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relayPos), relay.getBlockState(), relay
        );
        helper.assertTrue(
            collector.machineState().stored().add(relay.machineState().stored()).equals(EmcValue.of(4)),
            "A collector/relay feedback edge generated or lost EMC"
        );
        helper.assertTrue(relay.comparatorSignal() >= 0,
            "Machine comparator signal was outside its lower bound");
        helper.succeed();
    }

    @GameTest
    public void machineOwnershipAllowsOwnerServerMenu(GameTestHelper helper) {
        BlockPos relative = new BlockPos(9, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.COLLECTOR_MK2);
        EmcMachineBlockEntity machine = machine(helper, relative);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        machine.claim(owner.getUUID());
        BlockPos absolute = helper.absolutePos(relative);
        owner.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        helper.getBlockState(relative).useWithoutItem(
            helper.getLevel(), owner,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        helper.assertTrue(owner.containerMenu instanceof EmcMachineMenu,
            "Machine owner could not open the server menu");
        helper.assertTrue(machine.machineState().access().owner().orElseThrow().equals(owner.getUUID()),
            "Machine did not persist its owner identity");
        EmcMachineMenu menu = (EmcMachineMenu) owner.containerMenu;
        helper.assertTrue(menu.clickMenuButton(owner, 0)
                && machine.machineState().redstoneMode() == MachineRedstoneMode.REQUIRE_SIGNAL,
            "Server menu did not apply the owner redstone control");
        helper.assertTrue(menu.clickMenuButton(owner, 1)
                && machine.machineState().access().publicAccess(),
            "Server menu did not apply the owner access control");
        owner.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void relayMovesExactEmcBetweenKleinStarsAndExposesSidedStorage(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.RELAY_MK1);
        EmcMachineBlockEntity relay = machine(helper, relative);
        var horizontal = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.NORTH
        );
        var vertical = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.UP
        );
        helper.assertTrue(horizontal != null && vertical != null,
            "Fabric sided machine storages were not exposed");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertTrue(horizontal.insert(
                ItemVariant.of(star(ProjectEXItems.KLEIN_STAR_DREI.item(), 1_000)),
                1,
                transaction
            ) == 1, "Horizontal automation could not insert the relay input");
            helper.assertTrue(vertical.insert(
                ItemVariant.of(star(ProjectEXItems.KLEIN_STAR_DREI.item(), 0)),
                1,
                transaction
            ) == 1, "Vertical automation could not insert the relay output storage");
            transaction.commit();
        }

        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), relay.getBlockState(), relay
        );
        helper.assertTrue(stored(relay.getItem(EmcMachineBlockEntity.INPUT_SLOT)).equals(EmcValue.of(936)),
            "Relay input extraction was not exact");
        helper.assertTrue(stored(relay.getItem(EmcMachineBlockEntity.OUTPUT_SLOT)).equals(EmcValue.of(64)),
            "Relay output charge was not exact");
        helper.assertTrue(relay.machineState().stored().equals(EmcValue.ZERO),
            "Relay retained duplicated EMC after charging");
        helper.succeed();
    }

    @GameTest
    public void collectorRedstoneModeControlsServerGeneration(GameTestHelper helper) {
        BlockPos relative = new BlockPos(12, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.COLLECTOR_MK1);
        EmcMachineBlockEntity collector = machine(helper, relative);
        java.util.UUID owner = java.util.UUID.randomUUID();
        collector.claim(owner);
        helper.assertTrue(collector.setRedstoneMode(MachineRedstoneMode.REQUIRE_SIGNAL, owner, false),
            "Owner could not configure machine redstone policy");

        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), collector.getBlockState(), collector
        );
        helper.assertTrue(collector.machineState().stored().equals(EmcValue.ZERO),
            "Signal-required collector generated without redstone power");
        helper.setBlock(relative.relative(Direction.EAST), Blocks.REDSTONE_BLOCK);
        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), collector.getBlockState(), collector
        );
        helper.assertTrue(collector.machineState().stored().equals(EmcValue.of(4)),
            "Powered collector did not generate its exact rate");
        helper.succeed();
    }

    @GameTest
    public void collectorFuelUpgradeSpendsExactEmcDifference(GameTestHelper helper) {
        BlockPos relative = new BlockPos(15, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.COLLECTOR_MK3);
        EmcMachineBlockEntity collector = machine(helper, relative);
        collector.setItem(
            EmcMachineBlockEntity.INPUT_SLOT,
            new ItemStack(ProjectEXItems.ALCHEMICAL_COAL.item())
        );
        for (int tick = 0; tick < 100; tick++) {
            EmcMachineBlockEntity.tickServer(
                helper.getLevel(), helper.absolutePos(relative), collector.getBlockState(), collector
            );
        }
        EmcValue inputValue = ProjectEX.emc().find(EmcKey.parse("projectex:alchemical_coal"))
            .orElseThrow();
        EmcValue outputValue = ProjectEX.emc().find(EmcKey.parse("projectex:mobius_fuel"))
            .orElseThrow();
        EmcValue expectedStored = EmcValue.of(4_000).subtract(outputValue.subtract(inputValue));
        helper.assertTrue(collector.getItem(EmcMachineBlockEntity.OUTPUT_SLOT)
                .is(ProjectEXItems.MOBIUS_FUEL.item()),
            "Collector did not produce the next fuel tier");
        helper.assertTrue(collector.machineState().stored().equals(expectedStored),
            "Collector fuel upgrade did not spend the exact EMC difference");
        helper.succeed();
    }

    @GameTest
    public void machineStateAndInventoryRoundTripThroughBlockEntityPersistence(GameTestHelper helper) {
        BlockPos relative = new BlockPos(6, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.COLLECTOR_MK3);
        EmcMachineBlockEntity original = machine(helper, relative);
        original.setItem(EmcMachineBlockEntity.INPUT_SLOT,
            new ItemStack(ProjectEXItems.ALCHEMICAL_COAL.item(), 3));
        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), original.getBlockState(), original
        );

        var tag = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
            helper.absolutePos(relative),
            original.getBlockState(),
            tag,
            helper.getLevel().registryAccess()
        );
        helper.assertTrue(loaded instanceof EmcMachineBlockEntity,
            "Saved machine did not decode as its registered block-entity type");
        EmcMachineBlockEntity restored = (EmcMachineBlockEntity) loaded;
        helper.assertTrue(restored.tier() == MachineTier.COLLECTOR_MK3,
            "Machine tier changed across persistence");
        helper.assertTrue(restored.machineState().equals(original.machineState()),
            "Versioned machine state changed across persistence");
        helper.assertTrue(restored.getItem(EmcMachineBlockEntity.INPUT_SLOT).getCount()
                == original.getItem(EmcMachineBlockEntity.INPUT_SLOT).getCount(),
            "Machine inventory changed across persistence");

        ItemStack machineDrop = net.minecraft.world.level.block.Block.getDrops(
            original.getBlockState(), helper.getLevel(), helper.absolutePos(relative), original
        ).stream().filter(stack -> stack.is(ProjectEXBlocks.COLLECTOR_MK3.asItem()))
            .findFirst().orElseThrow(() -> new AssertionError("Machine block did not produce its stateful drop"));
        MachineItemState carried = machineDrop.get(ProjectEXComponents.MACHINE_STATE);
        helper.assertTrue(carried != null && carried.toMachineState().equals(original.machineState()),
            "Broken machine item did not retain exact machine state");
        BlockPos replacementPos = new BlockPos(7, 1, 0);
        helper.setBlock(replacementPos, ProjectEXBlocks.COLLECTOR_MK3);
        EmcMachineBlockEntity replaced = machine(helper, replacementPos);
        replaced.applyComponentsFromItemStack(machineDrop);
        helper.assertTrue(replaced.machineState().equals(original.machineState()),
            "Placed machine did not restore exact carried state");
        helper.assertTrue(replaced.getItem(EmcMachineBlockEntity.INPUT_SLOT).getCount()
                == original.getItem(EmcMachineBlockEntity.INPUT_SLOT).getCount(),
            "Placed machine did not restore its carried inventory");
        helper.succeed();
    }

    @GameTest
    public void condenserMk2ConservesExactEmcAndExposesSeparatedAutomation(GameTestHelper helper) {
        BlockPos relative = new BlockPos(18, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.CONDENSER_MK2);
        AlchemyStorageBlockEntity condenser = helper.getBlockEntity(relative, AlchemyStorageBlockEntity.class);
        condenser.setItem(AlchemyStorageBlockEntity.TARGET_SLOT, new ItemStack(Items.DIAMOND));
        condenser.setItem(1, new ItemStack(Items.COAL, 64));

        AlchemyStorageBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), condenser.getBlockState(), condenser
        );
        helper.assertTrue(condenser.getItem(1).isEmpty(), "Condenser did not consume its exact input batch");
        helper.assertTrue(condenser.getItem(43).is(Items.DIAMOND)
                && condenser.getItem(43).getCount() == 1,
            "Condenser did not produce exactly one diamond from 8192 EMC");
        helper.assertTrue(condenser.storageState().stored().equals(EmcValue.ZERO),
            "Condenser retained or generated EMC after an exact conversion");

        var horizontal = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.NORTH);
        var vertical = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.UP);
        helper.assertTrue(horizontal != null && vertical != null,
            "Condenser sided Fabric storage was not exposed");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertTrue(horizontal.insert(ItemVariant.of(new ItemStack(Items.COAL)), 1, transaction) == 1,
                "Horizontal automation could not insert condenser input");
            helper.assertTrue(vertical.extract(ItemVariant.of(new ItemStack(Items.DIAMOND)), 1, transaction) == 1,
                "Vertical automation could not extract condenser output");
            transaction.commit();
        }
        helper.succeed();
    }

    @GameTest
    public void fullCondenserOutputConsumesNothing(GameTestHelper helper) {
        BlockPos relative = new BlockPos(21, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.CONDENSER_MK1);
        AlchemyStorageBlockEntity condenser = helper.getBlockEntity(relative, AlchemyStorageBlockEntity.class);
        condenser.setItem(0, new ItemStack(Items.DIAMOND));
        condenser.setItem(1, new ItemStack(Items.COAL, 4));
        for (int slot = 43; slot < condenser.getContainerSize(); slot++) {
            condenser.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        AlchemyStorageBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), condenser.getBlockState(), condenser
        );
        helper.assertTrue(condenser.getItem(1).getCount() == 4,
            "A full condenser output consumed input");
        helper.assertTrue(condenser.storageState().stored().equals(EmcValue.ZERO),
            "A full condenser output changed its EMC buffer");
        helper.succeed();
    }

    @GameTest
    public void condenserRejectsStatefulTargetWithoutExactEmcDefinition(GameTestHelper helper) {
        BlockPos relative = new BlockPos(27, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.CONDENSER_MK2);
        AlchemyStorageBlockEntity condenser = helper.getBlockEntity(relative, AlchemyStorageBlockEntity.class);
        ItemStack namedDiamond = new ItemStack(Items.DIAMOND);
        namedDiamond.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Do not clone"));
        String exactComponents = io.github.tufkan1.projectex.api.fabric.MinecraftEmcAdapter
            .exactMatch(namedDiamond, helper.getLevel().registryAccess()).orElseThrow().componentsJson();
        helper.assertTrue(exactComponents != null && exactComponents.contains("minecraft:custom_name"),
            "Stateful target did not serialize to canonical EMC component JSON");
        condenser.setItem(0, namedDiamond);
        condenser.setItem(1, new ItemStack(Items.COAL, 64));
        AlchemyStorageBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), condenser.getBlockState(), condenser
        );
        helper.assertTrue(condenser.getItem(1).getCount() == 64,
            "Stateful target fell back to componentless EMC and consumed input");
        helper.assertTrue(condenser.getItem(43).isEmpty(),
            "Stateful target was cloned without an exact component EMC definition");
        helper.succeed();
    }

    @GameTest
    public void alchemicalChestBreakPlacePreservesOwnerAndAllPages(GameTestHelper helper) {
        BlockPos relative = new BlockPos(24, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.ALCHEMICAL_CHEST);
        AlchemyStorageBlockEntity chest = helper.getBlockEntity(relative, AlchemyStorageBlockEntity.class);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        chest.claim(owner.getUUID());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        chest.setItem(103, new ItemStack(Items.COAL, 7));
        BlockPos absolute = helper.absolutePos(relative);
        owner.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        helper.getBlockState(relative).useWithoutItem(
            helper.getLevel(), owner,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        helper.assertTrue(owner.containerMenu instanceof AlchemyStorageMenu,
            "Alchemical Chest did not open its server-owned paged menu");
        AlchemyStorageMenu chestMenu = (AlchemyStorageMenu) owner.containerMenu;
        helper.assertTrue(chestMenu.clickMenuButton(owner, 1) && chestMenu.page() == 1,
            "Alchemical Chest page change was not applied by the server menu");
        owner.closeContainer();

        ItemStack drop = net.minecraft.world.level.block.Block.getDrops(
            chest.getBlockState(), helper.getLevel(), helper.absolutePos(relative), chest
        ).stream().filter(stack -> stack.is(ProjectEXBlocks.ALCHEMICAL_CHEST.asItem()))
            .findFirst().orElseThrow();
        BlockPos replacement = new BlockPos(25, 1, 0);
        helper.setBlock(replacement, ProjectEXBlocks.ALCHEMICAL_CHEST);
        AlchemyStorageBlockEntity restored = helper.getBlockEntity(replacement, AlchemyStorageBlockEntity.class);
        restored.applyComponentsFromItemStack(drop);
        helper.assertTrue(restored.getItem(0).getCount() == 3 && restored.getItem(103).getCount() == 7,
            "Alchemical Chest lost a paged inventory entry during break/place");
        helper.assertTrue(restored.storageState().access().owner().orElseThrow().equals(owner.getUUID()),
            "Alchemical Chest lost ownership during break/place");
        helper.succeed();
    }

    @GameTest
    public void copiedBagIdentitySharesOneServerInventoryAndRejectsNesting(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AlchemicalBagItem bagItem = ProjectEXItems.alchemicalBags().getFirst().item();
        ItemStack original = new ItemStack(bagItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, original);
        bagItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu instanceof AlchemyStorageMenu,
            "Alchemical Bag did not open its server-owned menu");
        AlchemyStorageMenu first = (AlchemyStorageMenu) player.containerMenu;
        first.getSlot(1).set(new ItemStack(Items.DIAMOND, 5));
        helper.assertTrue(first.clickMenuButton(player, 1) && first.page() == 1,
            "Alchemical Bag page change was not server-owned");
        first.getSlot(1).set(new ItemStack(Items.COAL, 9));
        BagItemState identity = original.get(ProjectEXComponents.BAG_IDENTITY);
        helper.assertTrue(identity != null, "Bag did not acquire a persistent UUID");
        ItemStack copiedIdentity = original.copy();
        player.closeContainer();

        player.setItemInHand(InteractionHand.MAIN_HAND, copiedIdentity);
        bagItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        AlchemyStorageMenu mirrored = (AlchemyStorageMenu) player.containerMenu;
        helper.assertTrue(mirrored.getSlot(1).getItem().is(Items.DIAMOND)
                && mirrored.getSlot(1).getItem().getCount() == 5,
            "Copied bag identity created a duplicating second inventory instead of a mirror");
        helper.assertTrue(mirrored.clickMenuButton(player, 1)
                && mirrored.getSlot(1).getItem().is(Items.COAL)
                && mirrored.getSlot(1).getItem().getCount() == 9,
            "Copied bag identity did not mirror its second inventory page");
        helper.assertTrue(!mirrored.getSlot(2).mayPlace(new ItemStack(bagItem)),
            "Portable bag nesting was accepted");
        player.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void redMatterAreaMiningIsServerAuthoritativeProtectedAndExact(GameTestHelper helper) {
        BlockPos center = new BlockPos(12, 1, 6);
        BlockPos protectedRelative = center.offset(1, 0, 1);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            helper.setBlock(center.offset(x, 0, z), Blocks.STONE);
        }
        BlockPos centerAbsolute = helper.absolutePos(center);
        BlockPos protectedAbsolute = helper.absolutePos(protectedRelative);
        MatterAreaActionProtection.EVENT.register(
            context -> !context.position().equals(protectedAbsolute)
        );

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(centerAbsolute.getX() + 0.5, centerAbsolute.getY() + 2.0,
            centerAbsolute.getZ() + 0.5);
        player.setXRot(90.0F);
        player.setShiftKeyDown(true);
        ItemStack tool = new ItemStack(ProjectEXItems.RED_MATTER_PICKAXE.item());
        tool.set(ProjectEXComponents.MATTER_TOOL_STATE, new MatterToolState(1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MatterEmcPayment.credit(player, EmcValue.of(10_000));

        ProjectEXItems.RED_MATTER_PICKAXE.item().useOn(new UseOnContext(
            player,
            InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(centerAbsolute), Direction.UP, centerAbsolute, false)
        ));

        helper.assertBlockPresent(Blocks.STONE, protectedRelative);
        int removed = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (helper.getBlockState(center.offset(x, 0, z)).isAir()) removed++;
        }
        helper.assertValueEqual(removed, 8, "Area blocks committed");
        helper.assertTrue(MatterEmcPayment.balance(player).equals(EmcValue.of(9_744)),
            "Area mining did not debit exactly 32 EMC per committed block");
        helper.assertValueEqual(tool.getDamageValue(), 8, "Tool durability spent");
        helper.assertTrue(player.getCooldowns().isOnCooldown(tool),
            "Successful area mining did not start its server cooldown");
        helper.succeed();
    }

    @GameTest
    public void matterToolChargeAndArmorFamilyLoadAtRuntime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack tool = new ItemStack(ProjectEXItems.DARK_MATTER_PICKAXE.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ProjectEXItems.DARK_MATTER_PICKAXE.item().use(
            helper.getLevel(), player, InteractionHand.MAIN_HAND
        );
        helper.assertValueEqual(
            tool.getOrDefault(ProjectEXComponents.MATTER_TOOL_STATE, MatterToolState.DEFAULT).charge(),
            1,
            "Server-owned matter tool charge"
        );
        helper.assertValueEqual(ProjectEXItems.MATTER_HAND_TOOLS.size(), 8,
            "Matter hand-tool family size");
        helper.assertValueEqual(ProjectEXItems.MATTER_ARMOR.size(), 8,
            "Matter armor family size");
        helper.succeed();
    }

    @GameTest
    public void redMatterFurnaceSmeltsAtTierSpeedWithExactBonusAndRemainder(GameTestHelper helper) {
        BlockPos relative = new BlockPos(15, 1, 6);
        helper.setBlock(relative, ProjectEXBlocks.RED_MATTER_FURNACE);
        MatterFurnaceBlockEntity furnace = helper.getBlockEntity(relative, MatterFurnaceBlockEntity.class);
        var top = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), Direction.UP);
        var side = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), Direction.NORTH);
        helper.assertTrue(top != null && side != null, "Matter furnace sided inputs were not exposed");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(top.insert(ItemVariant.of(new ItemStack(Items.RAW_IRON)), 1, transaction),
                1L, "Top furnace input insertion");
            helper.assertValueEqual(side.insert(ItemVariant.of(new ItemStack(Items.LAVA_BUCKET)), 1, transaction),
                1L, "Side furnace fuel insertion");
            transaction.commit();
        }

        for (int tick = 0; tick < 5; tick++) {
            MatterFurnaceBlockEntity.tickServer(
                helper.getLevel(), helper.absolutePos(relative), furnace.getBlockState(), furnace
            );
        }

        helper.assertTrue(furnace.getItem(MatterFurnaceBlockEntity.INPUT_SLOT).isEmpty(),
            "Red matter furnace did not consume exactly one input");
        helper.assertTrue(furnace.getItem(MatterFurnaceBlockEntity.FUEL_SLOT).is(Items.BUCKET),
            "Matter furnace did not retain the exact lava-bucket remainder");
        helper.assertTrue(furnace.getItem(MatterFurnaceBlockEntity.OUTPUT_START).is(Items.IRON_INGOT)
                && furnace.getItem(MatterFurnaceBlockEntity.OUTPUT_START).getCount() == 2,
            "Red matter furnace did not commit its deterministic two-output result");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(relative);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        helper.getBlockState(relative).useWithoutItem(
            helper.getLevel(), player,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        helper.assertTrue(player.containerMenu instanceof MatterFurnaceMenu menu
                && menu.tier().id().equals(io.github.tufkan1.projectex.matter.MatterTier.RED.id()),
            "Red matter furnace did not open its synchronized accessible menu");
        player.closeContainer();

        ItemStack drop = net.minecraft.world.level.block.Block.getDrops(
            furnace.getBlockState(), helper.getLevel(), absolute, furnace
        ).stream().filter(stack -> stack.is(ProjectEXBlocks.RED_MATTER_FURNACE.asItem()))
            .findFirst().orElseThrow();
        BlockPos replacement = new BlockPos(16, 1, 6);
        helper.setBlock(replacement, ProjectEXBlocks.RED_MATTER_FURNACE);
        MatterFurnaceBlockEntity restored = helper.getBlockEntity(replacement, MatterFurnaceBlockEntity.class);
        restored.applyComponentsFromItemStack(drop);
        helper.assertTrue(restored.getItem(MatterFurnaceBlockEntity.OUTPUT_START).is(Items.IRON_INGOT)
                && restored.getItem(MatterFurnaceBlockEntity.OUTPUT_START).getCount() == 2,
            "Matter furnace lost its exact inventory during break/place");
        var bottom = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(replacement), Direction.DOWN
        );
        helper.assertTrue(bottom != null, "Matter furnace bottom output was not exposed");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(bottom.extract(
                ItemVariant.of(new ItemStack(Items.IRON_INGOT)), 1, transaction
            ), 1L, "Bottom furnace output extraction");
            transaction.commit();
        }
        helper.succeed();
    }

    @GameTest
    public void fullMatterFurnaceOutputConsumesNoFuelOrInput(GameTestHelper helper) {
        BlockPos relative = new BlockPos(18, 1, 6);
        helper.setBlock(relative, ProjectEXBlocks.RED_MATTER_FURNACE);
        MatterFurnaceBlockEntity furnace = helper.getBlockEntity(relative, MatterFurnaceBlockEntity.class);
        furnace.setItem(MatterFurnaceBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 3));
        furnace.setItem(MatterFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 2));
        for (int slot = MatterFurnaceBlockEntity.OUTPUT_START;
             slot < MatterFurnaceBlockEntity.OUTPUT_START + 18; slot++) {
            furnace.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }

        for (int tick = 0; tick < 10; tick++) {
            MatterFurnaceBlockEntity.tickServer(
                helper.getLevel(), helper.absolutePos(relative), furnace.getBlockState(), furnace
            );
        }

        helper.assertValueEqual(furnace.getItem(MatterFurnaceBlockEntity.INPUT_SLOT).getCount(), 3,
            "Blocked furnace input count");
        helper.assertValueEqual(furnace.getItem(MatterFurnaceBlockEntity.FUEL_SLOT).getCount(), 2,
            "Blocked furnace fuel count");
        helper.assertValueEqual(furnace.burnRemaining(), 0, "Blocked furnace burn time");
        helper.succeed();
    }

    private static EmcMachineBlockEntity machine(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockEntity(relative, EmcMachineBlockEntity.class);
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
