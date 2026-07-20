package io.github.tufkan1.projectex.matter;

import io.github.tufkan1.projectex.ProjectEX;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

/** Loads winning data-pack resources and publishes both tiers as one validated snapshot. */
public final class MatterTierReloadListener extends SimpleReloadListener<Map<String, MatterTier>> {
    private static final String DIRECTORY = "projectex/matter_tiers";

    @Override protected Map<String, MatterTier> prepare(PreparableReloadListener.SharedState state) {
        Map<String, MatterTier> tiers = new HashMap<>(MatterTier.DEFAULTS);
        java.util.Set<String> seen = new java.util.HashSet<>();
        Map<Identifier, Resource> resources = state.resourceManager().listResources(
            DIRECTORY, id -> id.getPath().endsWith(".json")
        );
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (Reader reader = entry.getValue().openAsReader()) {
                MatterTier tier = MatterTierDataParser.parse(reader);
                if (!MatterTier.DEFAULTS.containsKey(tier.id())) {
                    throw new IllegalArgumentException("Unknown matter tier id: " + tier.id());
                }
                if (!seen.add(tier.id())) {
                    throw new IllegalArgumentException("Duplicate winning definition for tier: " + tier.id());
                }
                if (tier.furnaceOutputSlots() > 13) {
                    throw new IllegalArgumentException("furnace_output_slots exceeds runtime inventory bound");
                }
                tiers.put(tier.id(), tier);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Could not read matter tier " + entry.getKey(), exception);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid matter tier " + entry.getKey() + ": "
                    + exception.getMessage(), exception);
            }
        });
        return Map.copyOf(tiers);
    }

    @Override protected void apply(
        Map<String, MatterTier> prepared, PreparableReloadListener.SharedState state
    ) {
        MatterTierConfig.publish(prepared);
        ProjectEX.LOGGER.info("Loaded {} validated matter tier definitions", prepared.size());
    }
}
