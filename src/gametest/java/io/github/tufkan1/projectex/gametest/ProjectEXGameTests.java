package io.github.tufkan1.projectex.gametest;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcStorageApi;
import io.github.tufkan1.projectex.api.storage.EmcStorageContext;
import io.github.tufkan1.projectex.api.storage.EmcTransferMode;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXContentRegistry;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.ExpansionMaterialTier;
import io.github.tufkan1.projectex.endgame.FinalStarAccess;
import io.github.tufkan1.projectex.api.endgame.FinalStarSlot;
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
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.content.AlchemicalBagItem;
import io.github.tufkan1.projectex.content.component.BagItemState;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.matter.MatterTierConfig;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import io.github.tufkan1.projectex.menu.AutomationMenu;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.core.registries.BuiltInRegistries;

/** Runtime registration, resource reload, and physical menu smoke tests. */
@SuppressWarnings("removal")
public final class ProjectEXGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void emcLinkUsesClaimedSidedTransactionalAccountStorage(GameTestHelper helper) {
        BlockPos relative = new BlockPos(19, 1, 6);
        helper.setBlock(relative, ProjectEXBlocks.EMC_LINKS.get(
            io.github.tufkan1.projectex.machine.ExpansionMachineTier.BASIC).block());
        AutomationBlockEntity link = helper.getBlockEntity(relative, AutomationBlockEntity.class);
        BlockPos absolute = helper.absolutePos(relative);
        helper.assertTrue(ItemStorage.SIDED.find(helper.getLevel(), absolute, Direction.DOWN) == null,
            "Unclaimed EMC Link exposed an offline account");

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        link.claim(owner.getUUID());
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(helper.getLevel().getServer());
        EmcKey cobblestone = EmcKey.parse("minecraft:cobblestone");
        data.update(owner.getUUID(), ignored ->
            new io.github.tufkan1.projectex.player.PlayerAlchemyState(
                EmcValue.of(100), new java.util.TreeSet<>(java.util.Set.of(cobblestone))
            ));

        var down = ItemStorage.SIDED.find(helper.getLevel(), absolute, Direction.DOWN);
        var up = ItemStorage.SIDED.find(helper.getLevel(), absolute, Direction.UP);
        helper.assertTrue(down != null && up != null, "Claimed EMC Link did not expose sided storage");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(down.insert(ItemVariant.of(Items.COBBLESTONE), 10, transaction),
                10L, "Down EMC Link insertion");
            transaction.abort();
        }
        helper.assertValueEqual(data.state(owner.getUUID()).balance(), EmcValue.of(100),
            "Aborted EMC Link insertion changed balance");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(down.insert(ItemVariant.of(Items.COBBLESTONE), 10, transaction),
                10L, "Committed EMC Link insertion");
            transaction.commit();
        }
        helper.assertValueEqual(data.state(owner.getUUID()).balance(), EmcValue.of(110),
            "Committed EMC Link insertion did not credit exact EMC");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(up.extract(ItemVariant.of(Items.COBBLESTONE), 10, transaction),
                10L, "Up EMC Link extraction");
            transaction.commit();
        }
        helper.assertValueEqual(data.state(owner.getUUID()).balance(), EmcValue.of(100),
            "Committed EMC Link extraction did not debit exact EMC");
        helper.succeed();
    }

    @GameTest
    public void transmutationInterfaceEnumeratesOnlyBoundKnowledgeAndPersistsOwner(GameTestHelper helper) {
        BlockPos relative = new BlockPos(20, 1, 6);
        helper.setBlock(relative, ProjectEXBlocks.TRANSMUTATION_INTERFACE);
        AutomationBlockEntity automation = helper.getBlockEntity(relative, AutomationBlockEntity.class);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        automation.claim(owner.getUUID());
        BlockPos absolute = helper.absolutePos(relative);
        owner.setPos(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5);
        helper.getBlockState(relative).useWithoutItem(helper.getLevel(), owner,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertTrue(owner.containerMenu instanceof AutomationMenu,
            "Transmutation Interface did not open its authorized management menu");
        helper.assertTrue(owner.containerMenu.clickMenuButton(owner, 0),
            "Owner could not update public-insert access setting");
        owner.closeContainer();
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(helper.getLevel().getServer());
        EmcKey diamond = EmcKey.parse("minecraft:diamond");
        data.update(owner.getUUID(), ignored ->
            new io.github.tufkan1.projectex.player.PlayerAlchemyState(
                EmcValue.of(8192), new java.util.TreeSet<>(java.util.Set.of(diamond))
            ));
        var storage = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(relative), Direction.UP
        );
        helper.assertTrue(storage != null && !storage.supportsInsertion(),
            "Transmutation Interface exposed an insertion path");
        java.util.List<ItemVariant> available = new java.util.ArrayList<>();
        storage.nonEmptyViews().forEach(view -> available.add(view.getResource()));
        helper.assertValueEqual(available, java.util.List.of(ItemVariant.of(Items.DIAMOND)),
            "Transmutation Interface knowledge availability");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(storage.extract(ItemVariant.of(Items.DIAMOND), 1, transaction),
                1L, "Transmutation Interface diamond extraction");
            transaction.commit();
        }
        helper.assertValueEqual(data.state(owner.getUUID()).balance(), EmcValue.ZERO,
            "Transmutation Interface did not spend server-priced EMC");

        ItemStack drop = net.minecraft.world.level.block.Block.getDrops(
            automation.getBlockState(), helper.getLevel(), helper.absolutePos(relative), automation
        ).stream().filter(stack -> stack.is(ProjectEXBlocks.TRANSMUTATION_INTERFACE.asItem()))
            .findFirst().orElseThrow();
        BlockPos replacement = new BlockPos(21, 1, 6);
        helper.setBlock(replacement, ProjectEXBlocks.TRANSMUTATION_INTERFACE);
        AutomationBlockEntity restored = helper.getBlockEntity(replacement, AutomationBlockEntity.class);
        restored.applyComponentsFromItemStack(drop);
        helper.assertValueEqual(restored.automationState().owner(), Optional.of(owner.getUUID()),
            "Automation owner was not retained through break/place");
        ServerPlayer thief = helper.makeMockServerPlayerInLevel();
        restored.placedBy(thief);
        helper.assertValueEqual(restored.automationState().owner(), Optional.of(thief.getUUID()),
            "Stolen automation block retained access to the previous offline account");
        helper.assertTrue(restored.automationState().members().isEmpty()
                && !restored.automationState().publicInsert(),
            "Stolen automation block retained the previous access configuration");
        helper.succeed();
    }

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
    public void tabletOpensSameSessionAndClosesWhenNoLongerHeld(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack tablet = new ItemStack(ProjectEXItems.TRANSMUTATION_TABLET.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);

        ProjectEXItems.TRANSMUTATION_TABLET.item().use(
            helper.getLevel(), player, InteractionHand.MAIN_HAND
        );
        helper.assertTrue(player.containerMenu instanceof TransmutationMenu,
            "Transmutation Tablet did not open the M2 server menu");
        TransmutationMenu menu = (TransmutationMenu) player.containerMenu;
        helper.assertTrue(menu.stillValid(player), "Held tablet menu was not authorized");
        player.setItemInHand(InteractionHand.MAIN_HAND,
            new ItemStack(ProjectEXItems.TRANSMUTATION_TABLET.item()));
        helper.assertTrue(!menu.stillValid(player),
            "Tablet session accepted a replacement item in the opening hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player),
            "Tablet session remained authorized after the tablet left the hand");
        player.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void repairTalismanRepairsInventoryWithoutStackingWork(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
        damaged.setDamageValue(10);
        player.getInventory().setItem(5, damaged);

        int repaired = io.github.tufkan1.projectex.content.RepairTalismanItem
            .repairInventory(player);
        helper.assertValueEqual(repaired, 1, "Repair Talisman repaired an unexpected item count");
        helper.assertValueEqual(damaged.getDamageValue(), 9,
            "Repair Talisman did not repair exactly one durability point");
        helper.succeed();
    }

    @GameTest
    public void diviningRodScanIsBoundedReadOnlyAndTierLimited(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack rod = new ItemStack(ProjectEXItems.DIVINING_RODS.get(2).item());
        rod.set(ProjectEXComponents.ACTIVE_ITEM_STATE,
            new ActiveItemState(ActiveItemState.CURRENT_VERSION, 2,
                io.github.tufkan1.projectex.content.component.ActiveItemMode.CUBE));
        BlockPos origin = new BlockPos(4, 3, 4);
        BlockPos ore = origin.below();
        BlockPos denied = ore.east();
        helper.setBlock(ore, Blocks.DIAMOND_ORE);
        helper.setBlock(denied, Blocks.BEDROCK);

        var result = io.github.tufkan1.projectex.content.DiviningRodItem.scan(
            helper.getLevel(), player, rod, helper.absolutePos(origin), Direction.UP
        );
        helper.assertTrue(result.scannedBlocks() > 0 && result.scannedBlocks() <= 9 * 64,
            "Divining Rod exceeded its bounded scan volume");
        helper.assertTrue(result.highest().size() <= 3,
            "High Divining Rod returned more than three distinct values");
        helper.assertTrue(!result.highest().isEmpty()
                && result.highest().getFirst().equals(EmcValue.of(8_192)),
            "Divining Rod did not resolve the diamond ore smelting fallback EMC");
        helper.assertBlockPresent(Blocks.DIAMOND_ORE, ore);
        helper.assertBlockPresent(Blocks.BEDROCK, denied);
        helper.succeed();
    }

    @GameTest
    public void darkMatterPedestalIsOwnedPersistentAndWorkBounded(GameTestHelper helper) {
        BlockPos relative = new BlockPos(6, 1, 6);
        helper.setBlock(relative, ProjectEXBlocks.DARK_MATTER_PEDESTAL);
        var pedestal = helper.getBlockEntity(relative,
            io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity.class);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer intruder = helper.makeMockServerPlayerInLevel();
        owner.setPos(Vec3.atCenterOf(helper.absolutePos(relative)));
        intruder.setPos(Vec3.atCenterOf(helper.absolutePos(relative)));
        pedestal.claim(owner.getUUID());

        ItemStack talisman = new ItemStack(ProjectEXItems.REPAIR_TALISMAN.item());
        helper.assertTrue(pedestal.insert(talisman, owner) && talisman.isEmpty(),
            "Owner could not insert one pedestal effect item");
        helper.assertTrue(!pedestal.cycleRedstoneMode(intruder)
                && pedestal.extract(intruder).isEmpty(),
            "Non-owner changed or extracted the pedestal");
        helper.assertTrue(pedestal.toggleActive(owner) && pedestal.comparatorSignal() == 15,
            "Owner could not activate the pedestal");

        ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
        damaged.setDamageValue(10);
        owner.getInventory().setItem(5, damaged);
        io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), pedestal.getBlockState(), pedestal);
        helper.assertValueEqual(damaged.getDamageValue(), 9,
            "Repair Talisman pedestal effect did not repair exactly one point");
        io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), pedestal.getBlockState(), pedestal);
        helper.assertValueEqual(damaged.getDamageValue(), 9,
            "Pedestal ignored its bounded effect cooldown");

        helper.assertTrue(pedestal.cycleRedstoneMode(owner)
                && pedestal.redstoneMode() == MachineRedstoneMode.REQUIRE_SIGNAL,
            "Owner could not cycle the pedestal redstone mode");
        var saved = pedestal.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
            helper.absolutePos(relative), pedestal.getBlockState(), saved,
            helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity copy
                && copy.active() && copy.item().is(ProjectEXItems.REPAIR_TALISMAN.item())
                && copy.access().owner().equals(pedestal.access().owner())
                && copy.redstoneMode() == MachineRedstoneMode.REQUIRE_SIGNAL,
            "Pedestal ownership, item, activation, or redstone mode did not persist");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                ProjectEX.id("dark_matter_pedestal"))).isPresent(),
            "Dark Matter Pedestal survival recipe is missing");
        helper.succeed();
    }

    @GameTest
    public void elementalAmuletsPlaceProtectedFluidsChargeExactlyAndControlWeather(
        GameTestHelper helper
    ) {
        BlockPos waterSupport = new BlockPos(9, 1, 5);
        BlockPos lavaSupport = new BlockPos(12, 1, 5);
        BlockPos deniedSupport = new BlockPos(15, 1, 5);
        helper.setBlock(waterSupport, Blocks.STONE);
        helper.setBlock(lavaSupport, Blocks.STONE);
        helper.setBlock(deniedSupport, Blocks.STONE);

        ServerPlayer waterPlayer = helper.makeMockServerPlayerInLevel();
        ServerPlayer lavaPlayer = helper.makeMockServerPlayerInLevel();
        ServerPlayer deniedPlayer = helper.makeMockServerPlayerInLevel();
        lavaPlayer.setGameMode(GameType.SURVIVAL);
        deniedPlayer.setGameMode(GameType.SURVIVAL);
        waterPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(waterSupport)));
        lavaPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(lavaSupport)));
        deniedPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(deniedSupport)));
        ItemStack evertide = new ItemStack(ProjectEXItems.EVERTIDE_AMULET.item());
        ItemStack volcanite = new ItemStack(ProjectEXItems.VOLCANITE_AMULET.item());
        ItemStack denied = new ItemStack(ProjectEXItems.VOLCANITE_AMULET.item());
        waterPlayer.setItemInHand(InteractionHand.MAIN_HAND, evertide);
        lavaPlayer.setItemInHand(InteractionHand.MAIN_HAND, volcanite);
        deniedPlayer.setItemInHand(InteractionHand.MAIN_HAND, denied);

        helper.assertTrue(useOnTop(helper, waterPlayer, waterSupport, evertide)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Evertide Amulet did not place a protected water source");
        helper.assertBlockPresent(Blocks.WATER, waterSupport.above());

        MatterEmcPayment.credit(lavaPlayer, EmcValue.of(31));
        helper.assertTrue(useOnTop(helper, lavaPlayer, lavaSupport, volcanite)
                == net.minecraft.world.InteractionResult.FAIL,
            "Volcanite Amulet placed lava without its exact EMC cost");
        helper.assertBlockPresent(Blocks.AIR, lavaSupport.above());
        MatterEmcPayment.credit(lavaPlayer, EmcValue.of(1));
        helper.assertTrue(useOnTop(helper, lavaPlayer, lavaSupport, volcanite)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Volcanite Amulet rejected an exactly funded placement");
        helper.assertBlockPresent(Blocks.LAVA, lavaSupport.above());
        helper.assertTrue(MatterEmcPayment.balance(lavaPlayer).equals(EmcValue.ZERO),
            "Volcanite Amulet did not debit exactly 32 EMC");

        io.github.tufkan1.projectex.api.utility.UtilityWorldActionProtection.EVENT.register(
            context -> !context.player().getUUID().equals(deniedPlayer.getUUID()));
        MatterEmcPayment.credit(deniedPlayer, EmcValue.of(32));
        helper.assertTrue(useOnTop(helper, deniedPlayer, deniedSupport, denied)
                == net.minecraft.world.InteractionResult.FAIL,
            "Elemental amulet ignored the claim protection callback");
        helper.assertBlockPresent(Blocks.AIR, deniedSupport.above());
        helper.assertTrue(MatterEmcPayment.balance(deniedPlayer).equals(EmcValue.of(32)),
            "Denied elemental action consumed EMC or left residual state");

        var evertideItem = (io.github.tufkan1.projectex.content.ElementalAmuletItem)
            ProjectEXItems.EVERTIDE_AMULET.item();
        var volcaniteItem = (io.github.tufkan1.projectex.content.ElementalAmuletItem)
            ProjectEXItems.VOLCANITE_AMULET.item();
        evertideItem.applyPedestalEffect(helper.getLevel(), helper.absolutePos(waterSupport),
            evertide, 4, 16);
        helper.assertTrue(helper.getLevel().getWeatherData().isRaining(),
            "Evertide pedestal effect did not start rain");
        volcaniteItem.applyPedestalEffect(helper.getLevel(), helper.absolutePos(lavaSupport),
            volcanite, 4, 16);
        helper.assertTrue(!helper.getLevel().getWeatherData().isRaining()
                && !helper.getLevel().getWeatherData().isThundering(),
            "Volcanite pedestal effect did not clear weather");
        for (String recipe : List.of("evertide_amulet", "volcanite_amulet")) {
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, ProjectEX.id(recipe)))
                .isPresent(), "Missing elemental amulet recipe: " + recipe);
        }
        helper.succeed();
    }

    @GameTest
    public void knowledgeTomeAtomicallyLearnsCurrentSnapshotAndPreservesEmc(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack tome = new ItemStack(ProjectEXItems.KNOWLEDGE_TOME.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, tome);
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(helper.getLevel().getServer());
        data.update(player.getUUID(), ignored -> new io.github.tufkan1.projectex.player.PlayerAlchemyState(
            EmcValue.of(123), new java.util.TreeSet<>(java.util.Set.of(
                EmcKey.parse("minecraft:diamond")))));
        java.util.Set<EmcKey> expected = ProjectEX.emc().snapshot().values().entrySet().stream()
            .filter(entry -> entry.getKey().componentsJson() == null
                && !entry.getValue().equals(EmcValue.ZERO))
            .map(entry -> entry.getKey().item()).collect(java.util.stream.Collectors.toSet());

        helper.assertTrue(ProjectEXItems.KNOWLEDGE_TOME.item().use(
                helper.getLevel(), player, InteractionHand.MAIN_HAND)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Knowledge Tome did not apply under the default consume policy");
        var after = data.state(player.getUUID());
        helper.assertTrue(after.knowledge().containsAll(expected)
                && after.knowledge().size() >= expected.size(),
            "Knowledge Tome did not atomically learn the current item-only EMC snapshot");
        helper.assertTrue(after.balance().equals(EmcValue.of(123)),
            "Knowledge Tome changed the player's EMC balance");
        helper.assertTrue(tome.isEmpty(), "Survival Knowledge Tome was not consumed");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                ProjectEX.id("knowledge_tome"))).isPresent(),
            "Knowledge Tome survival recipe is missing");
        helper.succeed();
    }

    @GameTest
    public void destructiveCatalystsAreBoundedProtectedAndExactlyCharged(GameTestHelper helper) {
        BlockPos origin = new BlockPos(9, 2, 10);
        helper.setBlock(origin, Blocks.STONE);
        helper.setBlock(origin.east(), Blocks.STONE);
        helper.setBlock(origin.west(), Blocks.BEDROCK);
        helper.setBlock(origin.north(), Blocks.CHEST);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(origin.above())));
        ItemStack destruction = new ItemStack(ProjectEXItems.DESTRUCTION_CATALYST.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, destruction);
        MatterEmcPayment.credit(player, EmcValue.of(8));

        helper.assertTrue(useOnTop(helper, player, origin, destruction)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Exactly funded Destruction Catalyst action failed");
        helper.assertBlockPresent(Blocks.AIR, origin);
        helper.assertBlockPresent(Blocks.STONE, origin.east());
        helper.assertBlockPresent(Blocks.BEDROCK, origin.west());
        helper.assertBlockPresent(Blocks.CHEST, origin.north());
        helper.assertTrue(MatterEmcPayment.balance(player).equals(EmcValue.ZERO),
            "Destruction Catalyst did not charge exactly 8 EMC for one committed block");
        helper.assertTrue(!destruction.isEmpty(), "Reusable Destruction Catalyst was consumed");

        BlockPos deniedOrigin = new BlockPos(14, 2, 10);
        helper.setBlock(deniedOrigin, Blocks.STONE);
        ServerPlayer deniedPlayer = helper.makeMockServerPlayerInLevel();
        deniedPlayer.setGameMode(GameType.SURVIVAL);
        deniedPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(deniedOrigin.above())));
        ItemStack deniedNova = new ItemStack(ProjectEXItems.NOVA_CATALYST.item());
        deniedPlayer.setItemInHand(InteractionHand.MAIN_HAND, deniedNova);
        io.github.tufkan1.projectex.api.utility.UtilityWorldActionProtection.EVENT.register(
            context -> !context.player().getUUID().equals(deniedPlayer.getUUID()));
        helper.assertTrue(useOnTop(helper, deniedPlayer, deniedOrigin, deniedNova)
                == net.minecraft.world.InteractionResult.FAIL,
            "Nova Catalyst ignored its per-target protection callback");
        helper.assertBlockPresent(Blocks.STONE, deniedOrigin);
        helper.assertTrue(!deniedNova.isEmpty(),
            "Denied Nova Catalyst action consumed the item or left residual state");

        BlockPos novaOrigin = new BlockPos(18, 2, 10);
        helper.setBlock(novaOrigin, Blocks.STONE);
        ServerPlayer novaPlayer = helper.makeMockServerPlayerInLevel();
        novaPlayer.setGameMode(GameType.SURVIVAL);
        novaPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(novaOrigin.above())));
        ItemStack nova = new ItemStack(ProjectEXItems.NOVA_CATALYST.item());
        novaPlayer.setItemInHand(InteractionHand.MAIN_HAND, nova);
        helper.assertTrue(useOnTop(helper, novaPlayer, novaOrigin, nova)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Nova Catalyst did not commit its bounded one-shot action");
        helper.assertBlockPresent(Blocks.AIR, novaOrigin);
        helper.assertTrue(nova.isEmpty(), "Committed Nova Catalyst was not consumed");
        for (String recipe : List.of("nova_catalyst", "destruction_catalyst")) {
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, ProjectEX.id(recipe)))
                .isPresent(), "Missing destructive catalyst recipe: " + recipe);
        }
        helper.succeed();
    }

    @GameTest
    public void vitalityStonesChargeAtomicallyAndProvideBoundedPedestalEffects(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(10.0F);
        player.getFoodData().setFoodLevel(10);
        ItemStack lifeStone = new ItemStack(ProjectEXItems.LIFE_STONE.item());
        player.setItemInHand(InteractionHand.MAIN_HAND, lifeStone);
        MatterEmcPayment.credit(player, EmcValue.of(127));
        helper.assertTrue(ProjectEXItems.LIFE_STONE.item().use(
                helper.getLevel(), player, InteractionHand.MAIN_HAND)
                == net.minecraft.world.InteractionResult.FAIL,
            "Life Stone partially applied without the atomic 128 EMC cost");
        helper.assertTrue(player.getHealth() == 10.0F
                && player.getFoodData().getFoodLevel() == 10
                && MatterEmcPayment.balance(player).equals(EmcValue.of(127)),
            "Failed Life Stone action changed vitality or EMC");
        MatterEmcPayment.credit(player, EmcValue.of(1));
        helper.assertTrue(ProjectEXItems.LIFE_STONE.item().use(
                helper.getLevel(), player, InteractionHand.MAIN_HAND)
                == net.minecraft.world.InteractionResult.SUCCESS_SERVER,
            "Exactly funded Life Stone action failed");
        helper.assertTrue(player.getHealth() == 12.0F
                && player.getFoodData().getFoodLevel() == 12
                && MatterEmcPayment.balance(player).equals(EmcValue.ZERO),
            "Life Stone did not apply both exact-cost effects");

        ServerPlayer nearby = helper.makeMockServerPlayerInLevel();
        BlockPos pedestal = new BlockPos(20, 2, 14);
        nearby.setPos(Vec3.atCenterOf(helper.absolutePos(pedestal)));
        nearby.setHealth(10.0F);
        nearby.getFoodData().setFoodLevel(10);
        ((io.github.tufkan1.projectex.content.VitalityStoneItem)
            ProjectEXItems.LIFE_STONE.item()).applyPedestalEffect(
                helper.getLevel(), helper.absolutePos(pedestal), lifeStone, 4, 16);
        helper.assertTrue(nearby.getHealth() == 11.0F
                && nearby.getFoodData().getFoodLevel() == 11,
            "Life Stone pedestal effect exceeded or missed its bounded one-point effects");
        for (String recipe : List.of("body_stone", "soul_stone", "life_stone")) {
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, ProjectEX.id(recipe)))
                .isPresent(), "Missing vitality stone recipe: " + recipe);
        }
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
            ProjectEXItems.materials().size() == 31
                && ProjectEXItems.EXPANSION_FUELS.size() == 11
                && ProjectEXItems.EXPANSION_MATTERS.size() == 11,
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
    public void expansionPowerFlowerUsesExactRateAndCompactSunMultiplier(GameTestHelper helper) {
        var basicFlower = ProjectEXBlocks.POWER_FLOWERS.get(
            io.github.tufkan1.projectex.machine.ExpansionMachineTier.BASIC
        ).block();
        BlockPos plainPos = new BlockPos(0, 2, 0);
        BlockPos absolutePlainPos = helper.absolutePos(plainPos);
        EmcMachineBlockEntity plain = new EmcMachineBlockEntity(
            absolutePlainPos, basicFlower.defaultBlockState()
        );
        plain.setLevel(helper.getLevel());

        EmcMachineBlockEntity.tickServer(
            helper.getLevel(), absolutePlainPos, plain.getBlockState(), plain
        );
        var persisted = plain.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
            helper.absolutePos(plainPos), plain.getBlockState(), persisted,
            helper.getLevel().registryAccess()
        );
        helper.assertTrue(loaded instanceof EmcMachineBlockEntity
                && ((EmcMachineBlockEntity) loaded).machineState().equals(plain.machineState()),
            "Power flower stored EMC or fractional remainder changed across reload");

        var plainSecond = io.github.tufkan1.projectex.machine.MachineRuntimeConfig
            .generationRate(plain.tier(), false)
            .generate(java.math.BigInteger.ZERO, 20, plain.tier().capacity());
        var boostedSecond = io.github.tufkan1.projectex.machine.MachineRuntimeConfig
            .generationRate(plain.tier(), true)
            .generate(java.math.BigInteger.ZERO, 20, plain.tier().capacity());
        helper.assertTrue(plainSecond.produced().equals(EmcValue.of(102))
                && plainSecond.deferredNumerator().signum() == 0,
            "Basic power flower lost its exact fixed-point second output");
        helper.assertTrue(boostedSecond.produced().equals(EmcValue.of(1_020))
                && boostedSecond.deferredNumerator().signum() == 0,
            "Compact Sun did not apply the validated default multiplier");
        for (var tier : io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()) {
            helper.assertTrue(ProjectEXBlocks.POWER_FLOWERS.containsKey(tier),
                "Missing registered power flower for " + tier.id());
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE,
                    ProjectEX.id(tier.id() + "_compressed_collector")
                )
            ).isPresent(), "Missing compressed collector recipe for " + tier.id());
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE,
                    ProjectEX.id(tier.id() + "_power_flower")
                )
            ).isPresent(), "Missing power flower recipe for " + tier.id());
        }
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE, ProjectEX.id("compact_sun")
            )
        ).isPresent(), "Missing Compact Sun recipe");
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
    public void finalCollectorUpgradesEveryExpansionFuelBoundaryExactly(GameTestHelper helper) {
        BlockPos relative = new BlockPos(16, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.EXPANSION_COLLECTORS.get(
            io.github.tufkan1.projectex.machine.ExpansionMachineTier.FINAL
        ).block());
        EmcMachineBlockEntity collector = machine(helper, relative);
        java.util.List<net.minecraft.world.item.Item> inputs = new java.util.ArrayList<>();
        inputs.add(ProjectEXItems.AETERNALIS_FUEL.item());
        inputs.addAll(ProjectEXItems.EXPANSION_FUELS.stream()
            .map(ProjectEXContentRegistry.RegisteredItem::item).limit(
                ProjectEXItems.EXPANSION_FUELS.size() - 1L
            ).toList());

        for (int index = 0; index < ProjectEXItems.EXPANSION_FUELS.size(); index++) {
            net.minecraft.world.item.Item input = inputs.get(index);
            net.minecraft.world.item.Item expectedOutput = ProjectEXItems.EXPANSION_FUELS.get(index).item();
            var before = collector.machineState();
            EmcValue inputValue = ProjectEX.emc().find(new EmcKey(
                BuiltInRegistries.ITEM.getKey(input).getNamespace(),
                BuiltInRegistries.ITEM.getKey(input).getPath()
            )).orElseThrow();
            EmcValue outputValue = ProjectEX.emc().find(new EmcKey(
                BuiltInRegistries.ITEM.getKey(expectedOutput).getNamespace(),
                BuiltInRegistries.ITEM.getKey(expectedOutput).getPath()
            )).orElseThrow();
            var generation = io.github.tufkan1.projectex.machine.MachineRuntimeConfig
                .generationRate(collector.tier()).generate(
                    before.deferredGeneration(), 1,
                    collector.tier().capacity().subtract(before.stored())
                );
            collector.setItem(EmcMachineBlockEntity.INPUT_SLOT, new ItemStack(input));
            EmcMachineBlockEntity.tickServer(
                helper.getLevel(), helper.absolutePos(relative), collector.getBlockState(), collector
            );

            helper.assertTrue(collector.getItem(EmcMachineBlockEntity.OUTPUT_SLOT).is(expectedOutput),
                "Collector missed expansion fuel boundary " + index);
            EmcValue expectedStored = before.stored().add(generation.produced())
                .subtract(outputValue.subtract(inputValue));
            helper.assertTrue(collector.machineState().stored().equals(expectedStored),
                "Expansion fuel boundary did not spend its exact EMC difference: " + index);
            helper.assertTrue(collector.machineState().deferredGeneration()
                    .equals(generation.deferredNumerator()),
                "Expansion fuel upgrade changed the fixed-point remainder: " + index);
            collector.removeItemNoUpdate(EmcMachineBlockEntity.OUTPUT_SLOT);
        }
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
    public void condenserMk3ProcessesOneBoundedExactBatch(GameTestHelper helper) {
        BlockPos relative = new BlockPos(20, 1, 0);
        helper.setBlock(relative, ProjectEXBlocks.CONDENSER_MK3);
        AlchemyStorageBlockEntity condenser = helper.getBlockEntity(relative, AlchemyStorageBlockEntity.class);
        condenser.setItem(AlchemyStorageBlockEntity.TARGET_SLOT, new ItemStack(Items.DIAMOND));
        for (int slot = 1; slot <= 8; slot++) {
            condenser.setItem(slot, new ItemStack(Items.COAL, 64));
        }

        AlchemyStorageBlockEntity.tickServer(
            helper.getLevel(), helper.absolutePos(relative), condenser.getBlockState(), condenser
        );

        for (int slot = 1; slot <= 8; slot++) {
            helper.assertTrue(condenser.getItem(slot).isEmpty(),
                "MK3 did not consume its complete bounded 512-item batch");
        }
        helper.assertTrue(condenser.getItem(92).is(Items.DIAMOND)
                && condenser.getItem(92).getCount() == 8,
            "MK3 did not produce exactly eight diamonds from 65,536 EMC");
        helper.assertTrue(condenser.storageState().stored().equals(EmcValue.ZERO),
            "MK3 exact batch left an invalid EMC remainder");
        helper.assertTrue(condenser.getContainerSize() == 272,
            "MK3 inventory layout does not preserve 91 inputs, 180 outputs, and one target");
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
    public void advancedAlchemicalChestMigratesLegacyStateAndExposesBoundedStorage(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(27, 1, 0);
        helper.setBlock(sourcePos, ProjectEXBlocks.ALCHEMICAL_CHEST);
        AlchemyStorageBlockEntity source = helper.getBlockEntity(sourcePos, AlchemyStorageBlockEntity.class);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        source.claim(owner.getUUID());
        source.setItem(0, new ItemStack(Items.DIAMOND, 3));
        source.setItem(103, new ItemStack(Items.COAL, 7));
        ItemStack carried = net.minecraft.world.level.block.Block.getDrops(
            source.getBlockState(), helper.getLevel(), helper.absolutePos(sourcePos), source
        ).stream().filter(stack -> stack.is(ProjectEXBlocks.ALCHEMICAL_CHEST.asItem()))
            .findFirst().orElseThrow();

        BlockPos upgradedPos = new BlockPos(29, 1, 0);
        helper.setBlock(upgradedPos, ProjectEXBlocks.ADVANCED_ALCHEMICAL_CHEST);
        AlchemyStorageBlockEntity upgraded = helper.getBlockEntity(upgradedPos, AlchemyStorageBlockEntity.class);
        // The smithing transform retains the base stack components; this exercises the same
        // component-to-block migration boundary without a client crafting screen.
        upgraded.applyComponentsFromItemStack(carried);

        helper.assertTrue(upgraded.getContainerSize() == 243,
            "Advanced Alchemical Chest did not expose its bounded 243-slot capacity");
        helper.assertTrue(upgraded.getItem(0).getCount() == 3 && upgraded.getItem(103).getCount() == 7,
            "Advanced Alchemical Chest migration lost a legacy inventory entry");
        helper.assertTrue(upgraded.storageState().access().owner().orElseThrow().equals(owner.getUUID()),
            "Advanced Alchemical Chest migration lost ownership");
        helper.assertTrue(upgraded.comparatorSignal() > 0,
            "Advanced Alchemical Chest comparator ignored migrated contents");

        var storage = ItemStorage.SIDED.find(
            helper.getLevel(), helper.absolutePos(upgradedPos), Direction.NORTH);
        helper.assertTrue(storage != null, "Advanced Alchemical Chest did not expose Fabric item storage");
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertTrue(storage.insert(ItemVariant.of(new ItemStack(Items.EMERALD)), 1, transaction) == 1,
                "Advanced Alchemical Chest rejected hopper-compatible insertion");
            transaction.commit();
        }
        AlchemyStorageMenu menu = new AlchemyStorageMenu(0, owner.getInventory(), upgraded);
        helper.assertTrue(menu.clickMenuButton(owner, 4) && menu.page() == 4,
            "Advanced Alchemical Chest fifth server-owned page was unreachable");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                ProjectEX.id("advanced_alchemical_chest")
            )
        ).isPresent(), "Advanced Alchemical Chest migration recipe is missing");
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

    @GameTest
    public void expandedFuelMatterChainIsMonotonicExactAndBurnable(GameTestHelper helper) {
        EmcValue previousFuel = ProjectEX.emc().find(EmcKey.parse("projectex:aeternalis_fuel"))
            .orElseThrow();
        EmcValue previousMatter = ProjectEX.emc().find(EmcKey.parse("projectex:red_matter"))
            .orElseThrow();
        for (ExpansionMaterialTier tier : ExpansionMaterialTier.values()) {
            var fuelItem = ProjectEXItems.EXPANSION_FUELS.get(tier.ordinal()).item();
            var matterItem = ProjectEXItems.EXPANSION_MATTERS.get(tier.ordinal()).item();
            EmcValue fuel = ProjectEX.emc().find(new EmcKey("projectex", tier.fuelId())).orElseThrow();
            EmcValue matter = ProjectEX.emc().find(new EmcKey("projectex", tier.matterId())).orElseThrow();
            helper.assertTrue(fuel.equals(previousFuel.multiply(4)),
                tier.fuelId() + " did not resolve to exactly four previous fuels");
            helper.assertTrue(matter.equals(fuel.multiply(6).add(previousMatter.multiply(3))),
                tier.matterId() + " did not resolve to its exact forward-only recipe cost");
            helper.assertValueEqual(helper.getLevel().fuelValues().burnDuration(new ItemStack(fuelItem)),
                25_600, tier.fuelId() + " burn duration");
            helper.assertTrue(matter.compareTo(previousMatter) > 0,
                tier.matterId() + " was not strictly more valuable than its predecessor");
            previousFuel = fuel;
            previousMatter = matter;
        }
        EmcValue fading = ProjectEX.emc().find(EmcKey.parse("projectex:fading_matter")).orElseThrow();
        helper.assertTrue(fading.equals(previousFuel.multiply(6).add(previousMatter.multiply(3))),
            "Fading Matter did not resolve to its exact terminal recipe cost");
        helper.succeed();
    }

    @GameTest
    public void infiniteSteakUsesAtomicEmcOrFinalStarLeaseWithoutShrinking(GameTestHelper helper) {
        for (String recipe : List.of("final_star_shard", "final_star", "infinite_steak")) {
            var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE, ProjectEX.id(recipe)
            );
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(key).isPresent(),
                "Missing endgame recipe: " + recipe);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack steak = new ItemStack(ProjectEXItems.INFINITE_STEAK.item());
        MatterEmcPayment.credit(player, EmcValue.of(128));
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0);

        ItemStack paidResult = steak.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(paidResult == steak && steak.getCount() == 1,
            "Infinite Steak shrank during paid use");
        helper.assertTrue(MatterEmcPayment.balance(player).equals(EmcValue.of(64)),
            "Infinite Steak did not debit its exact server EMC cost");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 18, "Paid Infinite Steak hunger");
        helper.assertTrue(player.getCooldowns().isOnCooldown(steak),
            "Infinite Steak did not apply its item cooldown");

        for (int tick = 0; tick < 20; tick++) player.getCooldowns().tick();
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0);
        player.getInventory().setItem(5, new ItemStack(ProjectEXItems.FINAL_STAR.item()));
        steak.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(MatterEmcPayment.balance(player).equals(EmcValue.of(64)),
            "Final Star backed use incorrectly debited player EMC");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 18, "Final Star Infinite Steak hunger");
        var capability = FinalStarAccess.find(player).orElseThrow();
        helper.assertTrue(capability.slot() == FinalStarSlot.INVENTORY && !capability.ready(),
            "Final Star inventory lease did not expose or claim its shared cooldown");
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

    private static net.minecraft.world.InteractionResult useOnTop(
        GameTestHelper helper, ServerPlayer player, BlockPos relativeSupport, ItemStack stack
    ) {
        BlockPos absolute = helper.absolutePos(relativeSupport);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
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
