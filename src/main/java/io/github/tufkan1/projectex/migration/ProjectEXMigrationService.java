package io.github.tufkan1.projectex.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import io.github.tufkan1.projectex.ProjectEX;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;

/** Bounded backup, dry-run, migration marker, and offline recovery-package service. */
public final class ProjectEXMigrationService {
    public static final int CURRENT_FORMAT = 1;
    private static final String BASELINE_RELEASE = "0.1.0-alpha.1";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter
        .ofPattern("uuuuMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private final Path worldRoot;
    private final Path configRoot;
    private final Clock clock;

    public ProjectEXMigrationService(Path worldRoot, Path configRoot) {
        this(worldRoot, configRoot, Clock.systemUTC());
    }

    ProjectEXMigrationService(Path worldRoot, Path configRoot, Clock clock) {
        this.worldRoot = normalize(worldRoot);
        this.configRoot = normalize(configRoot);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            MigrationReport report = new ProjectEXMigrationService(
                server.getWorldPath(LevelResource.ROOT), FabricLoader.getInstance().getConfigDir()).apply();
            ProjectEX.LOGGER.info("ProjectEX world format {} ready; backup={} files={}",
                report.targetFormat, report.backupId, report.files.size());
        });
    }

    public MigrationReport dryRun() {
        int source = readCurrentFormat();
        if (source > CURRENT_FORMAT) {
            throw new IllegalStateException("World ProjectEX format " + source
                + " is newer than supported format " + CURRENT_FORMAT);
        }
        return new MigrationReport(source, CURRENT_FORMAT, true, false, "", candidates());
    }

    public MigrationReport apply() {
        MigrationReport plan = dryRun();
        if (plan.sourceFormat == CURRENT_FORMAT) {
            return new MigrationReport(plan.sourceFormat, plan.targetFormat, false, true,
                readBackupId(), plan.files);
        }
        String backupId = publishBackup(plan);
        try {
            writeMarker(plan.targetFormat, backupId);
            return new MigrationReport(plan.sourceFormat, plan.targetFormat, false, true, backupId, plan.files);
        } catch (IOException exception) {
            throw new IllegalStateException("ProjectEX migration backup is complete but its format marker was not published",
                exception);
        }
    }

    public MigrationReport backup() {
        MigrationReport plan = dryRun();
        String backupId = publishBackup(plan);
        return new MigrationReport(plan.sourceFormat, plan.sourceFormat, false, true, backupId, plan.files);
    }

    private String publishBackup(MigrationReport plan) {
        String backupId = BACKUP_TIME.format(clock.instant()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path backupRoot = metadataRoot().resolve("backups");
        Path staging = checked(backupRoot, backupRoot.resolve(".tmp-" + backupId));
        Path completed = checked(backupRoot, backupRoot.resolve(backupId));
        try {
            Files.createDirectories(staging);
            for (Candidate candidate : plan.files) {
                Path sourceRoot = candidate.scope.equals("world") ? worldRoot : configRoot;
                Path destination = checked(staging, staging.resolve(candidate.scope).resolve(candidate.relativePath));
                Files.createDirectories(destination.getParent());
                Files.copy(checked(sourceRoot, sourceRoot.resolve(candidate.relativePath)), destination,
                    StandardCopyOption.COPY_ATTRIBUTES);
            }
            BackupManifest manifest = new BackupManifest(1, backupId, Instant.now(clock).toString(),
                plan.sourceFormat, plan.targetFormat, plan.files);
            Files.writeString(staging.resolve("manifest.json"), GSON.toJson(manifest), StandardCharsets.UTF_8);
            atomicMove(staging, completed);
            return backupId;
        } catch (IOException exception) {
            deleteStaging(staging, backupRoot);
            throw new IllegalStateException("ProjectEX backup failed before publication; canonical files were unchanged",
                exception);
        }
    }

    /** Creates a package for offline restore; it never overwrites a live world. */
    public Path prepareRecovery(String backupId) {
        if (!backupId.matches("[0-9]{8}-[0-9]{6}-[0-9a-f]{8}")) {
            throw new IllegalArgumentException("Invalid ProjectEX backup id");
        }
        Path backup = checked(metadataRoot().resolve("backups"), metadataRoot().resolve("backups").resolve(backupId));
        if (Files.isSymbolicLink(backup) || !Files.isRegularFile(
            backup.resolve("manifest.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Unknown ProjectEX backup: " + backupId);
        }
        Path recoveryRoot = metadataRoot().resolve("recovery");
        Path staging = checked(recoveryRoot, recoveryRoot.resolve(".tmp-" + backupId));
        Path completed = checked(recoveryRoot, recoveryRoot.resolve(backupId));
        try {
            if (Files.exists(completed)) return completed;
            copyTree(backup, staging);
            Files.writeString(staging.resolve("RESTORE.txt"),
                "Stop the server. Copy world/* into the world root and config/* into the config root. "
                    + "Keep this package until the restored server has saved cleanly.\n", StandardCharsets.UTF_8);
            atomicMove(staging, completed);
            return completed;
        } catch (IOException exception) {
            deleteStaging(staging, recoveryRoot);
            throw new IllegalStateException("Could not prepare ProjectEX recovery package", exception);
        }
    }

    public Path exportRecoveryPayload(String payload) {
        java.util.Objects.requireNonNull(payload, "payload");
        Path recoveryRoot = metadataRoot().resolve("recovery");
        Path target = checked(recoveryRoot, recoveryRoot.resolve(
            "player-alchemy-" + BACKUP_TIME.format(clock.instant()) + "-"
                + UUID.randomUUID().toString().substring(0, 8) + ".json"));
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(recoveryRoot);
            Files.writeString(temporary, payload, StandardCharsets.UTF_8);
            atomicMove(temporary, target);
            return target;
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new IllegalStateException("Could not export ProjectEX recovery payload", exception);
        }
    }

    public MigrationReport status() {
        MigrationReport report = dryRun();
        return new MigrationReport(report.sourceFormat, report.targetFormat, false,
            report.sourceFormat == report.targetFormat, readBackupId(), report.files);
    }

    private List<Candidate> candidates() {
        ArrayList<Candidate> result = new ArrayList<>();
        addMatching(result, "world", worldRoot, worldRoot.resolve("data"), "projectex", ".dat");
        addMatching(result, "config", configRoot, configRoot.resolve("projectex"), "", ".properties");
        result.sort(Comparator.comparing(Candidate::scope).thenComparing(Candidate::relativePath));
        return List.copyOf(result);
    }

    private static void addMatching(
        List<Candidate> target, String scope, Path root, Path directory, String prefix, String suffix
    ) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(directory)) {
            throw new IllegalStateException("ProjectEX migration directory cannot be a symbolic link: " + directory);
        }
        try (var paths = Files.list(directory)) {
            paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .filter(path -> path.getFileName().toString().startsWith(prefix))
                .filter(path -> path.getFileName().toString().endsWith(suffix)).forEach(path -> {
                    try {
                        target.add(new Candidate(scope, root.relativize(path).toString().replace('\\', '/'),
                            Files.size(path), sha256(path)));
                    } catch (IOException exception) {
                        throw new IllegalStateException("Could not inspect migration candidate " + path, exception);
                    }
                });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect ProjectEX migration directory " + directory, exception);
        }
    }

    private int readCurrentFormat() {
        Path marker = metadataRoot().resolve("migration.properties");
        if (!Files.exists(marker)) return 0;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(marker, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return Integer.parseInt(properties.getProperty("format", "-1"));
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Invalid ProjectEX migration marker " + marker, exception);
        }
    }

    private String readBackupId() {
        Path marker = metadataRoot().resolve("migration.properties");
        if (!Files.exists(marker)) return "";
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(marker, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties.getProperty("backup", "");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read ProjectEX migration marker", exception);
        }
    }

    private void writeMarker(int format, String backupId) throws IOException {
        Path marker = metadataRoot().resolve("migration.properties");
        Path temporary = marker.resolveSibling(marker.getFileName() + ".tmp");
        try {
            Files.createDirectories(marker.getParent());
            String contents = "# ProjectEX world format marker\nformat=" + format + "\nsource_release="
                + BASELINE_RELEASE + "\nbackup=" + backupId + "\ncompleted_at=" + Instant.now(clock) + "\n";
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            atomicMove(temporary, marker);
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw exception;
        }
    }

    private Path metadataRoot() { return checked(worldRoot, worldRoot.resolve("projectex")); }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0;) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("Recovery backup contains a symbolic link");
                Path destination = checked(target, target.resolve(source.relativize(path)));
                if (Files.isDirectory(path)) Files.createDirectories(destination);
                else Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private static void deleteStaging(Path staging, Path allowedRoot) {
        if (!staging.normalize().startsWith(allowedRoot.normalize()) || !staging.getFileName().toString().startsWith(".tmp-")) {
            return;
        }
        try {
            if (!Files.exists(staging)) return;
            try (var paths = Files.walk(staging)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        } catch (IOException ignored) { }
    }

    private static Path checked(Path root, Path path) {
        Path normalizedRoot = normalize(root);
        Path normalized = normalize(path);
        if (!normalized.startsWith(normalizedRoot)) throw new IllegalArgumentException("Path escapes ProjectEX scope");
        return normalized;
    }

    private static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    public record Candidate(String scope, String relativePath, long bytes, String sha256) { }
    public record MigrationReport(
        int sourceFormat, int targetFormat, boolean dryRun, boolean complete, String backupId, List<Candidate> files
    ) {
    }
    private record BackupManifest(
        int manifestVersion, String backupId, String createdAt, int sourceFormat, int targetFormat,
        List<Candidate> files
    ) { }
}
