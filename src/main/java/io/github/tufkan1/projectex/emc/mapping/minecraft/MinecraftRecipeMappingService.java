package io.github.tufkan1.projectex.emc.mapping.minecraft;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.emc.mapping.EmcMappingResult;
import io.github.tufkan1.projectex.emc.mapping.RecipeEmcMapper;
import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/** Rebuilds derived recipe values after Minecraft has completed a successful data reload. */
public final class MinecraftRecipeMappingService {
    private MinecraftRecipeMappingService() {
    }

    public static void register(EmcValueRegistry registry) {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                rebuild(server, registry);
            }
        });
    }

    static void rebuild(MinecraftServer server, EmcValueRegistry registry) {
        EmcSnapshot staged = registry.stagedSnapshot();
        ExplicitSnapshot explicit = explicitValues(staged.values(), staged.sources());
        Map<EmcMatch, EmcValue> explicitSnapshot = explicit.values();
        Map<EmcMatch, String> explicitSources = explicit.sources();
        Map<EmcKey, EmcValue> baseValues = new TreeMap<>();
        explicitSnapshot.forEach((match, value) -> {
            if (match.componentsJson() == null) {
                baseValues.put(match.item(), value);
            }
        });

        MinecraftRecipeAdapter.AdaptationResult adapted = MinecraftRecipeAdapter.adapt(
            server.getRecipeManager().getRecipes());
        EmcMappingResult mapped = RecipeEmcMapper.map(baseValues, adapted.recipes());

        Map<EmcMatch, EmcValue> combinedValues = new TreeMap<>(explicitSnapshot);
        Map<EmcMatch, String> combinedSources = new TreeMap<>(explicitSources);
        mapped.values().forEach((item, value) -> {
            EmcMatch match = EmcMatch.item(item);
            if (!explicitSnapshot.containsKey(match)) {
                combinedValues.put(match, value);
                combinedSources.put(match, "recipe:" + mapped.derivations().get(item).recipe());
            }
        });
        registry.replaceAll(combinedValues, combinedSources);
        ProjectEX.LOGGER.info(
            "Mapped {} recipes into {} derived EMC values; {} recipes unresolved and {} excluded",
            adapted.recipes().size(),
            mapped.derivations().size(),
            mapped.unresolvedRecipes().size(),
            adapted.exclusions().size()
        );
    }

    static ExplicitSnapshot explicitValues(
        Map<EmcMatch, EmcValue> currentValues,
        Map<EmcMatch, String> currentSources
    ) {
        Map<EmcMatch, EmcValue> values = new TreeMap<>();
        Map<EmcMatch, String> sources = new TreeMap<>();
        currentValues.forEach((match, value) -> {
            String source = currentSources.getOrDefault(match, "unknown");
            if (!source.startsWith("recipe:")) {
                values.put(match, value);
                sources.put(match, source);
            }
        });
        return new ExplicitSnapshot(Map.copyOf(values), Map.copyOf(sources));
    }

    record ExplicitSnapshot(Map<EmcMatch, EmcValue> values, Map<EmcMatch, String> sources) {
    }
}
