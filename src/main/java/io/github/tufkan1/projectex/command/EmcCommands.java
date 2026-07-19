package io.github.tufkan1.projectex.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcApi;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.menu.TransmutationMenu;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.server.commands.ReloadCommand;
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import io.github.tufkan1.projectex.config.ProjectEXConfig;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import io.github.tufkan1.projectex.migration.ProjectEXMigrationService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;
import io.github.tufkan1.projectex.emc.reload.EmcReloadDiagnostics;

/** Server-side ProjectEX diagnostics and operator commands. */
public final class EmcCommands {
    private EmcCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("projectex")
                .then(literal("status")
                    .executes(context -> status(context.getSource())))
                .then(literal("emc")
                    .then(argument("item", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            ProjectEX.emc().snapshot().values().keySet().stream()
                                .map(match -> match.item().toString())
                                .distinct(),
                            builder
                        ))
                        .executes(context -> query(
                            context.getSource(),
                            StringArgumentType.getString(context, "item")
                        ))))
                .then(literal("reload")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(context -> reload(context.getSource())))
                .then(literal("dump")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(context -> dump(context.getSource())))
                .then(literal("config")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(literal("status").executes(context -> configStatus(context.getSource())))
                    .then(literal("reload").executes(context -> configReload(context.getSource()))))
                .then(literal("datapack")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(literal("audit").executes(context -> datapackAudit(context.getSource()))))
                .then(literal("migration")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(literal("status").executes(context -> migrationStatus(context.getSource())))
                    .then(literal("report").executes(context -> migrationStatus(context.getSource())))
                    .then(literal("dry-run").executes(context -> migrationDryRun(context.getSource())))
                    .then(literal("backup").executes(context -> migrationBackup(context.getSource())))
                    .then(literal("apply").executes(context -> migrationApply(context.getSource())))
                    .then(literal("recovery")
                        .then(argument("backup", StringArgumentType.word()).executes(context -> prepareRecovery(
                            context.getSource(), StringArgumentType.getString(context, "backup"))))))
                .then(literal("machine")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(literal("audit")
                        .then(argument("pos", BlockPosArgument.blockPos()).executes(context -> auditMachine(
                            context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
                .then(literal("transmutation")
                    .executes(context -> openTransmutation(context.getSource())))
                .then(literal("automation")
                    .then(literal("member")
                        .then(argument("pos", BlockPosArgument.blockPos())
                            .then(argument("uuid", UuidArgument.uuid())
                                .then(literal("add").executes(context -> updateAutomationMember(
                                    context.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                    UuidArgument.getUuid(context, "uuid"), true)))
                                .then(literal("remove").executes(context -> updateAutomationMember(
                                    context.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                    UuidArgument.getUuid(context, "uuid"), false)))))))
                .then(literal("player")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(literal("inspect")
                        .then(argument("uuid", UuidArgument.uuid())
                            .executes(context -> inspectPlayer(
                                context.getSource(), UuidArgument.getUuid(context, "uuid")))))
                    .then(literal("reset")
                        .then(argument("uuid", UuidArgument.uuid())
                            .executes(context -> resetPlayer(
                                context.getSource(), UuidArgument.getUuid(context, "uuid")))))
                    .then(literal("recovery")
                        .executes(context -> recoveryStatus(context.getSource()))
                        .then(literal("status").executes(context -> recoveryStatus(context.getSource())))
                        .then(literal("export").executes(context -> exportRecovery(context.getSource())))
                        .then(literal("clear").executes(context -> clearRecovery(context.getSource()))))
                )
            )
        );
    }

    private static int status(net.minecraft.commands.CommandSourceStack source) {
        int count = ProjectEX.emc().snapshot().size();
        source.sendSuccess(() -> Component.translatable("commands.projectex.status", count), false);
        return count;
    }

    private static int query(net.minecraft.commands.CommandSourceStack source, String rawItem) {
        EmcKey key;
        try {
            key = EmcKey.parse(rawItem);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("commands.projectex.invalid_item", rawItem));
            return 0;
        }

        EmcMatch match = EmcMatch.item(key);
        EmcApi registry = ProjectEX.emc();
        Optional<EmcValue> value = registry.find(match);
        if (value.isEmpty()) {
            source.sendFailure(Component.translatable("commands.projectex.no_value", key.toString()));
            return 0;
        }
        String provenance = registry.snapshot().findSource(match).orElse("unknown");
        source.sendSuccess(() -> Component.translatable(
            "commands.projectex.value",
            key.toString(),
            value.orElseThrow().amount().toString(),
            provenance
        ), false);
        return 1;
    }

    private static int reload(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.projectex.reload_started"), true);
        ReloadCommand.reloadPacks(
            source.getServer().getPackRepository().getSelectedIds(),
            source
        );
        return 1;
    }

    private static int dump(net.minecraft.commands.CommandSourceStack source) {
        String report = EmcDiagnostics.toJson(ProjectEX.emc());
        ProjectEX.LOGGER.info("ProjectEX EMC diagnostics: {}", report);
        int count = ProjectEX.emc().snapshot().size();
        source.sendSuccess(() -> Component.translatable("commands.projectex.dumped", count), false);
        return count;
    }

    private static int inspectPlayer(net.minecraft.commands.CommandSourceStack source, UUID playerId) {
        PlayerAlchemyState state = PlayerAlchemySavedData.get(source.getServer()).state(playerId);
        source.sendSuccess(() -> Component.translatable(
            "commands.projectex.player.inspect",
            playerId.toString(),
            state.balance().amount().toString(),
            state.knowledge().size()
        ), false);
        return state.knowledge().size();
    }

    private static int openTransmutation(net.minecraft.commands.CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new TransmutationMenu(containerId, inventory, player),
            Component.translatable("menu.projectex.transmutation")
        )).isPresent() ? 1 : 0;
    }

    private static int resetPlayer(net.minecraft.commands.CommandSourceStack source, UUID playerId) {
        boolean removed = PlayerAlchemySavedData.get(source.getServer()).remove(playerId).isPresent();
        ProjectEX.LOGGER.warn("Operator {} reset ProjectEX player state for {} (existed={})",
            source.getTextName(), playerId, removed);
        source.sendSuccess(() -> Component.translatable(
            removed ? "commands.projectex.player.reset" : "commands.projectex.player.empty",
            playerId.toString()
        ), true);
        return removed ? 1 : 0;
    }

    private static int recoveryStatus(net.minecraft.commands.CommandSourceStack source) {
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(source.getServer());
        if (data.recoveryPayload().isPresent()) {
            String error = data.recoveryError().orElse("unknown");
            source.sendFailure(Component.translatable("commands.projectex.player.recovery_required", error));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.projectex.player.recovery_clean"), false);
        return 1;
    }

    private static int exportRecovery(net.minecraft.commands.CommandSourceStack source) {
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(source.getServer());
        if (data.recoveryPayload().isEmpty()) {
            source.sendFailure(Component.translatable("commands.projectex.player.recovery_missing"));
            return 0;
        }
        var path = migration(source).exportRecoveryPayload(data.recoveryPayload().orElseThrow());
        ProjectEX.LOGGER.warn("Operator {} exported ProjectEX recovery payload to {}",
            source.getTextName(), path);
        source.sendSuccess(() -> Component.translatable(
            "commands.projectex.player.recovery_exported", path.toString()), true);
        return 1;
    }

    private static int clearRecovery(net.minecraft.commands.CommandSourceStack source) {
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(source.getServer());
        if (data.recoveryPayload().isEmpty()) {
            source.sendFailure(Component.translatable("commands.projectex.player.recovery_missing"));
            return 0;
        }
        data.clearRecoveryBackup();
        ProjectEX.LOGGER.warn("Operator {} cleared the preserved ProjectEX player recovery payload",
            source.getTextName());
        source.sendSuccess(() -> Component.translatable("commands.projectex.player.recovery_cleared"), true);
        return 1;
    }

    private static int configStatus(net.minecraft.commands.CommandSourceStack source) {
        var report = ProjectEXConfig.report();
        source.sendSuccess(() -> Component.translatable("commands.projectex.config.status",
            report.schemaVersion(), report.settingCount(), report.files().size()), false);
        return report.settingCount();
    }

    private static int configReload(net.minecraft.commands.CommandSourceStack source) {
        try {
            var report = ProjectEXConfig.reloadServer();
            source.sendSuccess(() -> Component.translatable("commands.projectex.config.reloaded",
                report.settingCount()), true);
            return report.settingCount();
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("commands.projectex.config.failed", exception.getMessage()));
            return 0;
        }
    }

    private static int datapackAudit(net.minecraft.commands.CommandSourceStack source) {
        var snapshot = ProjectEX.emc().snapshot();
        var reload = EmcReloadDiagnostics.snapshot();
        if (!reload.successful()) {
            source.sendFailure(Component.translatable("commands.projectex.datapack.failed",
                reload.attempt(), reload.resourceCount(), reload.candidateCount(), reload.failure()));
            return 0;
        }
        long sources = snapshot.sources().values().stream().distinct().count();
        source.sendSuccess(() -> Component.translatable("commands.projectex.datapack.audit",
            snapshot.revision(), snapshot.size(), sources, reload.resourceCount(), reload.candidateCount()), false);
        return snapshot.size();
    }

    private static int migrationStatus(net.minecraft.commands.CommandSourceStack source) {
        return sendMigration(source, migration(source).status(), "commands.projectex.migration.status");
    }

    private static int migrationDryRun(net.minecraft.commands.CommandSourceStack source) {
        return sendMigration(source, migration(source).dryRun(), "commands.projectex.migration.dry_run");
    }

    private static int migrationApply(net.minecraft.commands.CommandSourceStack source) {
        var report = migration(source).apply();
        ProjectEX.LOGGER.warn("Operator {} applied ProjectEX migration {} -> {} backup={} files={}",
            source.getTextName(), report.sourceFormat(), report.targetFormat(), report.backupId(),
            report.files().size());
        return sendMigration(source, report, "commands.projectex.migration.applied");
    }

    private static int migrationBackup(net.minecraft.commands.CommandSourceStack source) {
        var report = migration(source).backup();
        ProjectEX.LOGGER.warn("Operator {} created ProjectEX backup {} with {} files",
            source.getTextName(), report.backupId(), report.files().size());
        return sendMigration(source, report, "commands.projectex.migration.backup");
    }

    private static int sendMigration(
        net.minecraft.commands.CommandSourceStack source,
        ProjectEXMigrationService.MigrationReport report,
        String translation
    ) {
        source.sendSuccess(() -> Component.translatable(translation, report.sourceFormat(),
            report.targetFormat(), report.files().size(), report.backupId()), false);
        return report.files().size() + 1;
    }

    private static int prepareRecovery(net.minecraft.commands.CommandSourceStack source, String backupId) {
        try {
            var path = migration(source).prepareRecovery(backupId);
            source.sendSuccess(() -> Component.translatable(
                "commands.projectex.migration.recovery", path.toString()), true);
            return 1;
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable(
                "commands.projectex.migration.recovery_failed", exception.getMessage()));
            return 0;
        }
    }

    private static int auditMachine(net.minecraft.commands.CommandSourceStack source, net.minecraft.core.BlockPos pos) {
        var blockEntity = source.getLevel().getBlockEntity(pos);
        if (blockEntity instanceof EmcMachineBlockEntity machine) {
            var state = machine.machineState();
            source.sendSuccess(() -> Component.translatable("commands.projectex.machine.audit",
                pos.toShortString(), machine.tier().name(), state.stored().amount().toString(),
                state.access().owner().map(UUID::toString).orElse("unclaimed"), state.access().publicAccess()), false);
            return 1;
        }
        if (blockEntity instanceof AlchemyStorageBlockEntity storage) {
            var state = storage.storageState();
            source.sendSuccess(() -> Component.translatable("commands.projectex.machine.audit",
                pos.toShortString(), "ALCHEMY_STORAGE", state.stored().amount().toString(),
                state.access().owner().map(UUID::toString).orElse("unclaimed"), state.access().publicAccess()), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.projectex.machine.not_found", pos.toShortString()));
        return 0;
    }

    private static ProjectEXMigrationService migration(net.minecraft.commands.CommandSourceStack source) {
        return new ProjectEXMigrationService(
            source.getServer().getWorldPath(LevelResource.ROOT),
            FabricLoader.getInstance().getConfigDir()
        );
    }

    private static int updateAutomationMember(
        net.minecraft.commands.CommandSourceStack source,
        net.minecraft.core.BlockPos pos,
        UUID member,
        boolean enabled
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        if (!(source.getLevel().getBlockEntity(pos) instanceof AutomationBlockEntity automation)
            || !automation.setMember(member, enabled, actor)) {
            source.sendFailure(Component.translatable("commands.projectex.automation.member.denied"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
            enabled ? "commands.projectex.automation.member.added"
                : "commands.projectex.automation.member.removed",
            member.toString()
        ), false);
        return 1;
    }
}
