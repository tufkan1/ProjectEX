package io.github.tufkan1.projectex.emc.reload;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.emc.data.EmcDataException;
import io.github.tufkan1.projectex.emc.data.EmcDataFile;
import io.github.tufkan1.projectex.emc.data.EmcDataParser;
import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

/** Loads every server data resource under projectex/emc and atomically publishes it. */
public final class EmcDataReloadListener extends SimpleReloadListener<ResolvedEmcData> {
    private static final String DIRECTORY = "projectex/emc";

    private final EmcValueRegistry registry;

    public EmcDataReloadListener(EmcValueRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected ResolvedEmcData prepare(PreparableReloadListener.SharedState state) {
        Map<Identifier, List<Resource>> resources = state.resourceManager().listResourceStacks(
            DIRECTORY,
            identifier -> identifier.getPath().endsWith(".json")
        );
        List<EmcDataSource> sources = new ArrayList<>();
        try {
            resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> parseStack(entry.getKey(), entry.getValue(), sources));
            ResolvedEmcData resolved = EmcDataResolver.resolve(sources);
            EmcReloadDiagnostics.success(resources.size(), sources.size(), resolved.values().size());
            return resolved;
        } catch (RuntimeException exception) {
            EmcReloadDiagnostics.failure(resources.size(), sources.size(), exception);
            throw exception;
        }
    }

    @Override
    protected void apply(ResolvedEmcData prepared, PreparableReloadListener.SharedState state) {
        registry.stageAll(prepared.values(), prepared.sources());
        ProjectEX.LOGGER.info("Loaded {} EMC values from {} winning data sources",
            prepared.values().size(), prepared.sources().values().stream().distinct().count());
    }

    private static void parseStack(Identifier identifier, List<Resource> stack, List<EmcDataSource> output) {
        stack.stream()
            .sorted(Comparator.comparing(Resource::sourcePackId))
            .forEach(resource -> {
                String sourceId = resource.sourcePackId() + "@" + identifier;
                try (Reader reader = resource.openAsReader()) {
                    EmcDataFile file = EmcDataParser.parse(reader);
                    output.add(new EmcDataSource(sourceId, file));
                } catch (IOException exception) {
                    throw new EmcDataException("Could not read EMC resource " + sourceId, exception);
                } catch (EmcDataException exception) {
                    throw new EmcDataException("Invalid EMC resource " + sourceId + ": "
                        + exception.getMessage(), exception);
                }
            });
    }
}
