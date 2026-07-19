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
}
