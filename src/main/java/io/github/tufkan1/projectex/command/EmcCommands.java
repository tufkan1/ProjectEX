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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.server.commands.ReloadCommand;

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
                .then(literal("transmutation")
                    .executes(context -> openTransmutation(context.getSource())))
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
                        .executes(context -> recoveryStatus(context.getSource())))
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
}
