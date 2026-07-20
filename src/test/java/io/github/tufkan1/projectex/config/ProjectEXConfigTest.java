package io.github.tufkan1.projectex.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.machine.MachineRuntimeConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectEXConfigTest {
    @TempDir Path directory;

    @AfterEach void reset() { ProjectEXConfig.resetForTest(); }

    @Test
    void createsCommentedVersionedSchemasAndPublishesValidatedSettings() throws Exception {
        var report = ProjectEXConfig.loadForTest(directory, true);

        assertEquals(1, report.schemaVersion());
        assertEquals(17, report.settingCount());
        assertEquals(65_536, MachineRuntimeConfig.networkBudget().maxTransfers());
        String server = Files.readString(directory.resolve("server.properties"));
        assertTrue(server.contains("schema_version=1"));
        assertTrue(server.contains("# Maximum machine transfers"));
        assertTrue(Files.isRegularFile(directory.resolve("common.properties")));
    }

    @Test
    void invalidReloadReportsExactPathAndKeyWithoutPublishingPartialState() throws Exception {
        ProjectEXConfig.loadForTest(directory, true);
        int before = MachineRuntimeConfig.networkBudget().maxTransfers();
        Path server = directory.resolve("server.properties");
        Files.writeString(server, Files.readString(server).replace(
            "projectex.machine.maxTransfersPerWorldTick=65536",
            "projectex.machine.maxTransfersPerWorldTick=0"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> ProjectEXConfig.loadForTest(directory, true));

        assertTrue(failure.getMessage().contains(server.toAbsolutePath().toString()));
        assertTrue(failure.getMessage().contains("projectex.machine.maxTransfersPerWorldTick"));
        assertEquals(before, MachineRuntimeConfig.networkBudget().maxTransfers());
    }

    @Test
    void clientSchemaIsSeparateAndControlsOnlyLocalPreferencePersistence() throws Exception {
        var report = ProjectEXConfig.loadClientForTest(directory);
        assertEquals(4, report.settingCount());
        assertTrue(ProjectEXConfig.rememberFavorites());
        Path client = directory.resolve("client.properties");
        Files.writeString(client, Files.readString(client).replace(
            "projectex.client.rememberFavorites=true", "projectex.client.rememberFavorites=false"));
        ProjectEXConfig.loadClientForTest(directory);
        assertTrue(!ProjectEXConfig.rememberFavorites());

        ProjectEXConfig.saveClientOptions(new ProjectEXConfig.ClientOptions(
            true, false, false, false));
        assertTrue(ProjectEXConfig.rememberFavorites());
        assertTrue(!ProjectEXConfig.showEmcTooltips());
        assertTrue(!ProjectEXConfig.compactEmcNumbers());
        assertTrue(!ProjectEXConfig.focusTransmutationSearch());
        String saved = Files.readString(client);
        assertTrue(saved.contains("projectex.client.showEmcTooltips=false"));
    }
}
