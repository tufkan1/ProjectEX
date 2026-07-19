package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.Reader;
import java.util.Map;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

/** Loads one winning complete star-tier table and atomically publishes it. */
public final class KleinStarTierReloadListener
    extends SimpleReloadListener<Map<KleinStarTier, EmcValue>> {
    private static final Identifier RESOURCE = ProjectEX.id("projectex/star_tiers.json");

    @Override protected Map<KleinStarTier, EmcValue> prepare(PreparableReloadListener.SharedState state) {
        Resource resource = state.resourceManager().getResource(RESOURCE)
            .orElseThrow(() -> new IllegalArgumentException("Missing required star tier table " + RESOURCE));
        try (Reader reader = resource.openAsReader()) {
            return KleinStarTierDataParser.parse(reader);
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Could not read star tier table " + RESOURCE, exception);
        }
    }

    @Override protected void apply(
        Map<KleinStarTier, EmcValue> prepared, PreparableReloadListener.SharedState state
    ) {
        KleinStarTierConfig.publish(prepared);
        ProjectEX.LOGGER.info("Loaded {} validated portable star tiers", prepared.size());
    }
}
