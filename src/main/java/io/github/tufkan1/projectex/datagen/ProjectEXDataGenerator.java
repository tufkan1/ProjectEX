package io.github.tufkan1.projectex.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/** Registers every reproducible ProjectEX data and resource provider. */
public final class ProjectEXDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ProjectEXRecipeProvider::new);
        pack.addProvider(ProjectEXBlockTagProvider::new);
        pack.addProvider(ProjectEXBlockLootProvider::new);
        pack.addProvider(ProjectEXEnglishLanguageProvider::new);
        pack.addProvider(ProjectEXAdvancementProvider::new);
        pack.addProvider(ProjectEXModelProvider::new);
    }
}
