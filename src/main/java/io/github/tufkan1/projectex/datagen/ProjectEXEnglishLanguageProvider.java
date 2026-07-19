package io.github.tufkan1.projectex.datagen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

/** Builds en_us from the review-friendly source translation template. */
public final class ProjectEXEnglishLanguageProvider extends FabricLanguageProvider {
    private static final Path SOURCE = Path.of("src", "main", "datagen", "lang", "en_us.json");

    public ProjectEXEnglishLanguageProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generateTranslations(
        HolderLookup.Provider registries,
        TranslationBuilder builder
    ) {
        try {
            builder.add(locateSource());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + SOURCE, exception);
        }
        for (var entry : io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_COLLECTORS.entrySet()) {
            builder.add(entry.getValue().block(), tierName(entry.getKey()) + " Collector [MK "
                + entry.getKey().level() + "]");
        }
        for (var entry : io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_RELAYS.entrySet()) {
            builder.add(entry.getValue().block(), tierName(entry.getKey()) + " Relay [MK "
                + entry.getKey().level() + "]");
        }
        for (var entry : io.github.tufkan1.projectex.content.ProjectEXBlocks.POWER_FLOWERS.entrySet()) {
            builder.add(entry.getValue().block(), tierName(entry.getKey()) + " Power Flower [MK "
                + entry.getKey().level() + "]");
        }
        for (int index = 0; index < io.github.tufkan1.projectex.content.ProjectEXItems
            .COMPRESSED_COLLECTORS.size(); index++) {
            var tier = io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()[index];
            builder.add(
                io.github.tufkan1.projectex.content.ProjectEXItems.COMPRESSED_COLLECTORS.get(index).item(),
                tierName(tier) + " Compressed Collector [MK " + tier.level() + "]"
            );
        }
        builder.add(io.github.tufkan1.projectex.content.ProjectEXBlocks.COMPACT_SUN, "Compact Sun");
        for (var entry : io.github.tufkan1.projectex.content.ProjectEXBlocks.EMC_LINKS.entrySet()) {
            builder.add(entry.getValue().block(), tierName(entry.getKey()) + " EMC Link [MK "
                + entry.getKey().level() + "]");
        }
        builder.add(io.github.tufkan1.projectex.content.ProjectEXBlocks.TRANSMUTATION_INTERFACE,
            "Transmutation Interface");
    }

    private static Path locateSource() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve(SOURCE);
            if (java.nio.file.Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Could not locate ProjectEX root containing " + SOURCE);
    }

    private static String tierName(io.github.tufkan1.projectex.machine.ExpansionMachineTier tier) {
        String id = tier.id();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }
}
