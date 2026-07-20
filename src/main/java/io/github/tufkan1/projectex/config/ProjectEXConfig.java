package io.github.tufkan1.projectex.config;

import io.github.tufkan1.projectex.content.DestructiveCatalystPolicy;
import io.github.tufkan1.projectex.content.KnowledgeTomePolicy;
import io.github.tufkan1.projectex.endgame.EndgameRuntimeConfig;
import io.github.tufkan1.projectex.knowledge.KnowledgeSharingConfig;
import io.github.tufkan1.projectex.machine.MachineRuntimeConfig;
import io.github.tufkan1.projectex.machine.MachineRateMultiplier;
import io.github.tufkan1.projectex.teleport.AlchemicalBookConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;

/** Versioned, validated and transactionally published common/server/client configuration. */
public final class ProjectEXConfig {
    public static final int SCHEMA_VERSION = 1;
    public static final String CLIENT_REMEMBER_FAVORITES = "projectex.client.rememberFavorites";
    public static final String CLIENT_SHOW_EMC_TOOLTIPS = "projectex.client.showEmcTooltips";
    public static final String CLIENT_COMPACT_EMC = "projectex.client.compactEmcNumbers";
    public static final String CLIENT_FOCUS_SEARCH = "projectex.client.focusTransmutationSearch";
    private static final String VERSION_KEY = "schema_version";
    private static final Set<String> MANAGED_PROPERTIES = new HashSet<>();
    private static final Schema COMMON = new Schema("common.properties", List.of(
        entry(KnowledgeTomePolicy.PROPERTY, "consume",
            "Knowledge Tome: disabled, consume, or operator_only.",
            oneOf("disabled", "consume", "operator_only")),
        entry(DestructiveCatalystPolicy.ENABLED_PROPERTY, "true",
            "Allow destructive catalyst world actions.", ProjectEXConfig::isBoolean)
    ));
    private static final Schema SERVER = new Schema("server.properties", List.of(
        entry(MachineRuntimeConfig.MAX_TRANSFERS_PROPERTY, "65536",
            "Maximum machine transfers per world tick (1..1000000).", integer(1, 1_000_000)),
        entry(MachineRuntimeConfig.MAX_EMC_PROPERTY,
            "115792089237316195423570985008687907853269984665640564039457584007913129639936",
            "Maximum total EMC moved by machines per world tick.", canonicalPositive(4096)),
        entry(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY, "10",
            "Compact Sun power-flower multiplier (0 is normalized to 1).", integer(0, 1_000_000)),
        entry(MachineRuntimeConfig.COLLECTOR_RATE_MULTIPLIER_PROPERTY, "1",
            "Collector rate multiplier as decimal or fraction.", ProjectEXConfig::isPositiveRate),
        entry(MachineRuntimeConfig.RELAY_TRANSFER_MULTIPLIER_PROPERTY, "1",
            "Relay transfer multiplier as decimal or fraction.", ProjectEXConfig::isPositiveRate),
        entry(MachineRuntimeConfig.POWER_FLOWER_RATE_MULTIPLIER_PROPERTY, "1",
            "Power Flower rate multiplier as decimal or fraction.", ProjectEXConfig::isPositiveRate),
        entry(EndgameRuntimeConfig.FINAL_STAR_ENABLED, "true", "Enable Final Star capabilities.",
            ProjectEXConfig::isBoolean),
        entry(EndgameRuntimeConfig.FINAL_STAR_SLOTS, "main_hand,off_hand,inventory",
            "Comma-separated Final Star slots.", value -> value.matches(
                "(main_hand|off_hand|inventory)(,(main_hand|off_hand|inventory))*")),
        entry(EndgameRuntimeConfig.FINAL_STAR_COOLDOWN, "20",
            "Final Star shared cooldown in ticks.", integer(1, 72_000)),
        entry(EndgameRuntimeConfig.CONSUMABLES_ENABLED, "true", "Enable infinite consumables.",
            ProjectEXConfig::isBoolean),
        entry(EndgameRuntimeConfig.STEAK_COST, "64", "Infinite Steak EMC cost.",
            canonicalPositive(80)),
        entry(EndgameRuntimeConfig.STEAK_COOLDOWN, "20", "Infinite Steak cooldown in ticks.",
            integer(1, 72_000)),
        entry(KnowledgeSharingConfig.POLICY, "enabled",
            "Knowledge sharing: enabled, creative_only, or disabled.",
            oneOf("enabled", "creative_only", "disabled")),
        entry(KnowledgeSharingConfig.LIFETIME_HOURS, "24",
            "Signed knowledge snapshot lifetime in hours (1..168).", integer(1, 168)),
        entry(AlchemicalBookConfig.EDIT_POLICY, "owner_only",
            "Bound Alchemical Book editing: owner_only, operator_only, or enabled.",
            oneOf("owner_only", "operator_only", "enabled"))
    ));
    private static final Schema CLIENT = new Schema("client.properties", List.of(
        entry(CLIENT_REMEMBER_FAVORITES, "true", "Persist transmutation favorites on this client.",
            ProjectEXConfig::isBoolean),
        entry(CLIENT_SHOW_EMC_TOOLTIPS, "true", "Show authoritative EMC values in item tooltips.",
            ProjectEXConfig::isBoolean),
        entry(CLIENT_COMPACT_EMC, "true", "Format large EMC values with K/M/B/T suffixes.",
            ProjectEXConfig::isBoolean),
        entry(CLIENT_FOCUS_SEARCH, "true", "Focus the transmutation search box when its screen opens.",
            ProjectEXConfig::isBoolean)
    ));
    private static volatile Path directory;
    private static volatile boolean rememberFavorites = true;
    private static volatile boolean showEmcTooltips = true;
    private static volatile boolean compactEmcNumbers = true;
    private static volatile boolean focusTransmutationSearch = true;

    private ProjectEXConfig() { }

    public static synchronized void initializeServer() {
        directory = FabricLoader.getInstance().getConfigDir().resolve("projectex");
        publish(List.of(COMMON, SERVER), true);
    }

    public static synchronized void initializeClient() {
        if (directory == null) directory = FabricLoader.getInstance().getConfigDir().resolve("projectex");
        publish(List.of(CLIENT), false);
        reloadClientRuntime();
    }

    public static synchronized ReloadReport reloadServer() {
        ensureInitialized();
        publish(List.of(COMMON, SERVER), true);
        return report(List.of(COMMON, SERVER));
    }

    public static synchronized ReloadReport report() {
        ensureInitialized();
        return report(List.of(COMMON, SERVER));
    }

    public static boolean rememberFavorites() { return rememberFavorites; }
    public static boolean showEmcTooltips() { return showEmcTooltips; }
    public static boolean compactEmcNumbers() { return compactEmcNumbers; }
    public static boolean focusTransmutationSearch() { return focusTransmutationSearch; }

    public static ClientOptions clientOptions() {
        return new ClientOptions(rememberFavorites, showEmcTooltips, compactEmcNumbers,
            focusTransmutationSearch);
    }

    public static synchronized void saveClientOptions(ClientOptions options) {
        ensureInitialized();
        java.util.Objects.requireNonNull(options, "options");
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(CLIENT_REMEMBER_FAVORITES, Boolean.toString(options.rememberFavorites()));
        values.put(CLIENT_SHOW_EMC_TOOLTIPS, Boolean.toString(options.showEmcTooltips()));
        values.put(CLIENT_COMPACT_EMC, Boolean.toString(options.compactEmcNumbers()));
        values.put(CLIENT_FOCUS_SEARCH, Boolean.toString(options.focusTransmutationSearch()));
        writeValues(directory.resolve(CLIENT.fileName), CLIENT, values);
        publish(List.of(CLIENT), false);
        reloadClientRuntime();
    }

    static synchronized ReloadReport loadForTest(Path root, boolean runtime) {
        directory = root;
        publish(List.of(COMMON, SERVER), runtime);
        return report(List.of(COMMON, SERVER));
    }

    static synchronized ReloadReport loadClientForTest(Path root) {
        directory = root;
        publish(List.of(CLIENT), false);
        reloadClientRuntime();
        return report(List.of(CLIENT));
    }

    static synchronized void resetForTest() {
        MANAGED_PROPERTIES.forEach(System::clearProperty);
        MANAGED_PROPERTIES.clear();
        directory = null;
        rememberFavorites = true;
        showEmcTooltips = true;
        compactEmcNumbers = true;
        focusTransmutationSearch = true;
        reloadRuntime();
    }

    private static void publish(List<Schema> schemas, boolean reloadRuntime) {
        ensureDirectory();
        LinkedHashMap<String, String> candidate = new LinkedHashMap<>();
        for (Schema schema : schemas) candidate.putAll(readOrCreate(schema));
        Map<String, String> previous = new HashMap<>();
        Set<String> previouslyManaged = Set.copyOf(MANAGED_PROPERTIES);
        candidate.forEach((key, value) -> previous.put(key, System.getProperty(key)));
        try {
            candidate.forEach((key, value) -> {
                if (System.getProperty(key) == null || MANAGED_PROPERTIES.contains(key)) {
                    System.setProperty(key, value);
                    MANAGED_PROPERTIES.add(key);
                }
            });
            validateEffective(schemas);
            if (reloadRuntime) reloadRuntime();
        } catch (RuntimeException exception) {
            previous.forEach((key, value) -> {
                if (value == null) System.clearProperty(key);
                else System.setProperty(key, value);
            });
            MANAGED_PROPERTIES.clear();
            MANAGED_PROPERTIES.addAll(previouslyManaged);
            if (reloadRuntime) reloadRuntime();
            throw exception;
        }
    }

    private static Map<String, String> readOrCreate(Schema schema) {
        Path path = directory.resolve(schema.fileName);
        if (!Files.exists(path)) writeDefaults(path, schema);
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid(path, "file", "could not be read", exception);
        }
        Set<String> allowed = new HashSet<>();
        allowed.add(VERSION_KEY);
        schema.entries.forEach(value -> allowed.add(value.key));
        for (String key : properties.stringPropertyNames()) {
            if (!allowed.contains(key)) throw invalid(path, key, "unknown key", null);
        }
        String version = properties.getProperty(VERSION_KEY);
        if (!Integer.toString(SCHEMA_VERSION).equals(version)) {
            throw invalid(path, VERSION_KEY, "expected " + SCHEMA_VERSION + " but found " + version, null);
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        boolean migratedClientOptions = false;
        for (Entry entry : schema.entries) {
            String value = properties.getProperty(entry.key);
            if (value == null && schema == CLIENT) {
                value = entry.defaultValue;
                migratedClientOptions = true;
            }
            if (value == null) throw invalid(path, entry.key, "required key is missing", null);
            value = value.trim();
            if (!entry.validator.test(value)) throw invalid(path, entry.key, "invalid value '" + value + "'", null);
            values.put(entry.key, value);
        }
        if (migratedClientOptions) writeValues(path, schema, values);
        return values;
    }

    private static void validateEffective(List<Schema> schemas) {
        for (Schema schema : schemas) {
            Path path = directory.resolve(schema.fileName);
            for (Entry entry : schema.entries) {
                String value = effective(entry.key, entry.defaultValue);
                if (!entry.validator.test(value)) {
                    throw invalid(path, entry.key, "invalid JVM override '" + value + "'", null);
                }
            }
        }
    }

    private static void reloadRuntime() {
        MachineRuntimeConfig.reload();
        EndgameRuntimeConfig.reload();
        KnowledgeTomePolicy.reload();
        DestructiveCatalystPolicy.reload();
        KnowledgeSharingConfig.reload();
        AlchemicalBookConfig.reload();
    }

    private static ReloadReport report(List<Schema> schemas) {
        ArrayList<String> files = new ArrayList<>();
        int settings = 0;
        for (Schema schema : schemas) {
            Path path = directory.resolve(schema.fileName);
            if (Files.exists(path)) files.add(path.toAbsolutePath().normalize().toString());
            settings += schema.entries.size();
        }
        return new ReloadReport(SCHEMA_VERSION, settings, List.copyOf(files));
    }

    private static void writeDefaults(Path path, Schema schema) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        schema.entries.forEach(entry -> values.put(entry.key, entry.defaultValue));
        writeValues(path, schema, values);
    }

    private static void writeValues(Path path, Schema schema, Map<String, String> values) {
        StringBuilder output = new StringBuilder();
        output.append("# ProjectEX ").append(schema.fileName).append("\n");
        output.append("# Unknown keys and invalid values stop loading; files are never silently reset.\n");
        output.append(VERSION_KEY).append('=').append(SCHEMA_VERSION).append("\n\n");
        for (Entry entry : schema.entries) {
            output.append("# ").append(entry.comment).append("\n");
            output.append(entry.key).append('=').append(values.getOrDefault(entry.key, entry.defaultValue))
                .append("\n\n");
        }
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, output.toString(), StandardCharsets.UTF_8);
            atomicMove(temporary, path);
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw invalid(path, "file", "could not write defaults", exception);
        }
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static IllegalArgumentException invalid(Path path, String key, String message, Exception cause) {
        String detail = "Invalid ProjectEX config " + path.toAbsolutePath().normalize()
            + ": key '" + key + "' " + message;
        return cause == null ? new IllegalArgumentException(detail) : new IllegalArgumentException(detail, cause);
    }

    private static Entry entry(String key, String fallback, String comment, Predicate<String> validator) {
        return new Entry(key, fallback, comment, validator);
    }

    private static Predicate<String> oneOf(String... values) {
        Set<String> allowed = Set.of(values);
        return allowed::contains;
    }

    private static Predicate<String> integer(int minimum, int maximum) {
        return value -> {
            try {
                int parsed = Integer.parseInt(value);
                return parsed >= minimum && parsed <= maximum;
            } catch (NumberFormatException exception) {
                return false;
            }
        };
    }

    private static Predicate<String> canonicalPositive(int digits) {
        return value -> value.length() <= digits && value.matches("[1-9][0-9]*");
    }

    private static boolean isBoolean(String value) { return value.equals("true") || value.equals("false"); }

    private static boolean isPositiveRate(String value) {
        try {
            MachineRateMultiplier.parse(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String effective(String key, String fallback) {
        return System.getProperty(key, fallback).trim();
    }

    private static void reloadClientRuntime() {
        rememberFavorites = Boolean.parseBoolean(effective(CLIENT_REMEMBER_FAVORITES, "true"));
        showEmcTooltips = Boolean.parseBoolean(effective(CLIENT_SHOW_EMC_TOOLTIPS, "true"));
        compactEmcNumbers = Boolean.parseBoolean(effective(CLIENT_COMPACT_EMC, "true"));
        focusTransmutationSearch = Boolean.parseBoolean(effective(CLIENT_FOCUS_SEARCH, "true"));
    }

    private static void ensureInitialized() {
        if (directory == null) throw new IllegalStateException("ProjectEX config has not been initialized");
    }

    private static void ensureDirectory() {
        ensureInitialized();
        try { Files.createDirectories(directory); }
        catch (IOException exception) { throw invalid(directory, "directory", "could not be created", exception); }
    }

    public record ReloadReport(int schemaVersion, int settingCount, List<String> files) { }
    public record ClientOptions(boolean rememberFavorites, boolean showEmcTooltips,
                                boolean compactEmcNumbers, boolean focusTransmutationSearch) {
        public static final ClientOptions DEFAULT = new ClientOptions(true, true, true, true);
    }
    private record Schema(String fileName, List<Entry> entries) { }
    private record Entry(String key, String defaultValue, String comment, Predicate<String> validator) { }
}
