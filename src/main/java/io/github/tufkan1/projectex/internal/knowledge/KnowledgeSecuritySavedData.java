package io.github.tufkan1.projectex.internal.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.ProjectEX;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-persistent signing secret, replay ledger, and bounded security audit. */
public final class KnowledgeSecuritySavedData extends SavedData {
    public static final int MAX_REPLAYS = 10_000;
    public static final int MAX_AUDIT_EVENTS = 512;
    static final Codec<KnowledgeSecuritySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("secret").forGetter(data -> Base64.getEncoder().encodeToString(data.secret)),
        Codec.STRING.listOf(0, MAX_REPLAYS).optionalFieldOf("consumed", List.of())
            .forGetter(KnowledgeSecuritySavedData::encodedConsumed),
        Codec.STRING.listOf(0, MAX_AUDIT_EVENTS).optionalFieldOf("audit", List.of())
            .forGetter(data -> List.copyOf(data.audit))
    ).apply(instance, KnowledgeSecuritySavedData::decode));
    private static final SavedDataType<KnowledgeSecuritySavedData> TYPE = new SavedDataType<>(
        ProjectEX.id("knowledge_security"), KnowledgeSecuritySavedData::new, CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final byte[] secret;
    private final TreeMap<UUID, Long> consumed;
    private final ArrayList<String> audit;

    public KnowledgeSecuritySavedData() {
        this(randomSecret(), Map.of(), List.of());
        setDirty();
    }

    private KnowledgeSecuritySavedData(byte[] secret, Map<UUID, Long> consumed, List<String> audit) {
        if (secret.length != 32) throw new IllegalArgumentException("Invalid knowledge signing secret");
        this.secret = secret.clone();
        this.consumed = new TreeMap<>(consumed);
        this.audit = new ArrayList<>(audit);
    }

    public static KnowledgeSecuritySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized byte[] secret() { return secret.clone(); }
    public synchronized Map<UUID, Long> consumed() { return Map.copyOf(consumed); }

    public synchronized void replaceConsumed(Map<UUID, Long> replacement) {
        consumed.clear();
        replacement.entrySet().stream().sorted(Map.Entry.comparingByValue()).skip(
            Math.max(0, replacement.size() - MAX_REPLAYS)).forEach(entry ->
                consumed.put(entry.getKey(), entry.getValue()));
        setDirty();
    }

    public synchronized void audit(String event) {
        String safe = event.replace('\n', ' ').replace('\r', ' ');
        while (audit.size() >= MAX_AUDIT_EVENTS) audit.remove(0);
        audit.add(safe.length() > 1_024 ? safe.substring(0, 1_024) : safe);
        setDirty();
    }

    public synchronized List<String> auditEvents() { return List.copyOf(audit); }

    private synchronized List<String> encodedConsumed() {
        return consumed.entrySet().stream()
            .map(entry -> entry.getKey() + "," + entry.getValue()).toList();
    }

    private static KnowledgeSecuritySavedData decode(String secret, List<String> consumed, List<String> audit) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        TreeMap<UUID, Long> ledger = new TreeMap<>();
        for (String entry : consumed) {
            int separator = entry.indexOf(',');
            if (separator <= 0) throw new IllegalArgumentException("Malformed knowledge replay entry");
            ledger.put(UUID.fromString(entry.substring(0, separator)), Long.parseLong(entry.substring(separator + 1)));
        }
        return new KnowledgeSecuritySavedData(decoded, ledger, audit);
    }

    private static byte[] randomSecret() {
        byte[] result = new byte[32];
        new SecureRandom().nextBytes(result);
        return result;
    }
}
