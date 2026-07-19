package io.github.tufkan1.projectex;

import io.github.tufkan1.projectex.api.emc.EmcValueRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main Fabric entrypoint. Keep loader wiring here and domain logic in testable packages. */
public final class ProjectEX implements ModInitializer {
    public static final String MOD_ID = "projectex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final EmcValueRegistry EMC_VALUES = new EmcValueRegistry();

    @Override
    public void onInitialize() {
        LOGGER.info("ProjectEX {} is initializing with {} EMC values", version(), EMC_VALUES.size());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static EmcValueRegistry emcValues() {
        return EMC_VALUES;
    }

    private static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("development");
    }
}
