package io.github.tufkan1.projectex.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectEXMigrationServiceTest {
    @TempDir Path directory;

    @Test
    void dryRunIsReadOnlyAndApplyPublishesBackupBeforeMarker() throws Exception {
        Path world = directory.resolve("world");
        Path config = directory.resolve("config");
        Files.createDirectories(world.resolve("data"));
        Files.createDirectories(config.resolve("projectex"));
        Files.writeString(world.resolve("data/projectex_player_alchemy.dat"), "player-data");
        Files.writeString(config.resolve("projectex/server.properties"), "schema_version=1\n");
        var service = new ProjectEXMigrationService(world, config,
            Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC));

        var dryRun = service.dryRun();
        assertTrue(dryRun.dryRun());
        assertEquals(0, dryRun.sourceFormat());
        assertEquals(2, dryRun.files().size());
        assertFalse(Files.exists(world.resolve("projectex")));

        var applied = service.apply();
        assertTrue(applied.complete());
        assertTrue(applied.backupId().startsWith("20260719-120000-"));
        Path backup = world.resolve("projectex/backups").resolve(applied.backupId());
        assertTrue(Files.isRegularFile(backup.resolve("manifest.json")));
        assertEquals("player-data", Files.readString(
            backup.resolve("world/data/projectex_player_alchemy.dat")));
        assertTrue(Files.readString(world.resolve("projectex/migration.properties")).contains("format=1"));
        assertEquals(applied.backupId(), service.status().backupId());
        assertTrue(service.apply().complete());
        assertEquals(applied.backupId(), service.apply().backupId());

        Path recovery = service.prepareRecovery(applied.backupId());
        assertTrue(Files.isRegularFile(recovery.resolve("RESTORE.txt")));
        assertThrows(IllegalArgumentException.class, () -> service.prepareRecovery("../escape"));
    }

    @Test
    void refusesWorldsFromAnewerFormat() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve("projectex"));
        Files.writeString(world.resolve("projectex/migration.properties"), "format=2\n");
        var service = new ProjectEXMigrationService(world, directory.resolve("config"));
        assertThrows(IllegalStateException.class, service::dryRun);
    }
}
