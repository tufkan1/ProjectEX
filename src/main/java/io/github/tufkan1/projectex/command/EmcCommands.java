package io.github.tufkan1.projectex.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.emc.EmcValueRegistry;
import java.util.Optional;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
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
                            ProjectEX.emcValues().snapshot().keySet().stream()
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
            )
        );
    }

    private static int status(net.minecraft.commands.CommandSourceStack source) {
        int count = ProjectEX.emcValues().size();
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
        EmcValueRegistry registry = ProjectEX.emcValues();
        Optional<EmcValue> value = registry.find(match);
        if (value.isEmpty()) {
            source.sendFailure(Component.translatable("commands.projectex.no_value", key.toString()));
            return 0;
        }
        String provenance = registry.findSource(match).orElse("unknown");
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
        String report = EmcDiagnostics.toJson(ProjectEX.emcValues());
        ProjectEX.LOGGER.info("ProjectEX EMC diagnostics: {}", report);
        int count = ProjectEX.emcValues().size();
        source.sendSuccess(() -> Component.translatable("commands.projectex.dumped", count), false);
        return count;
    }
}
