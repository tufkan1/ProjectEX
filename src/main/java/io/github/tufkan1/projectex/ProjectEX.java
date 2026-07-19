package io.github.tufkan1.projectex;

import io.github.tufkan1.projectex.api.emc.EmcApi;
import io.github.tufkan1.projectex.command.EmcCommands;
import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.emc.mapping.minecraft.MinecraftRecipeMappingService;
import io.github.tufkan1.projectex.emc.reload.EmcDataReloadListener;
import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import io.github.tufkan1.projectex.network.AlchemyNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main Fabric entrypoint. Keep loader wiring here and domain logic in testable packages. */
public final class ProjectEX implements ModInitializer {
    public static final String MOD_ID = "projectex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final EmcValueRegistry EMC_VALUES = new EmcValueRegistry();

    @Override
    public void onInitialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            id("emc_values"),
            new EmcDataReloadListener(EMC_VALUES)
        );
        ProjectEXMenus.register();
        EmcCommands.register();
        AlchemyNetworking.register();
        MinecraftRecipeMappingService.register(EMC_VALUES);
        LOGGER.info("ProjectEX {} is initializing with {} EMC values", version(), EMC_VALUES.snapshot().size());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Stable query-only entry point for other mods. */
    public static EmcApi emc() {
        return EMC_VALUES;
    }

    private static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("development");
    }
}
