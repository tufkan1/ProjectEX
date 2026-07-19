package io.github.tufkan1.projectex.automation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Set;

/** Strict schema-v1 persistence boundary for EMC automation ownership and access lists. */
public final class AutomationAccessCodec {
    public static final int CURRENT_VERSION = 1;

    private AutomationAccessCodec() {
    }

    public static Map<String, String> encode(AutomationAccess access) {
        Map<String, String> encoded = new LinkedHashMap<>();
        encoded.put("version", Integer.toString(CURRENT_VERSION));
        encoded.put("owner", access.owner().toString());
        encoded.put("members", access.members().stream().map(UUID::toString)
            .collect(java.util.stream.Collectors.joining(",")));
        encoded.put("public_insert", Boolean.toString(access.publicInsert()));
        return Map.copyOf(encoded);
    }

    public static AutomationAccess decode(Map<String, String> encoded) {
        try {
            if (!encoded.keySet().equals(Set.of("version", "owner", "members", "public_insert"))) {
                throw new IllegalArgumentException("Unexpected automation access fields");
            }
            if (Integer.parseInt(required(encoded, "version")) != CURRENT_VERSION) {
                throw new IllegalArgumentException("Unsupported automation access version");
            }
            UUID owner = UUID.fromString(required(encoded, "owner"));
            TreeSet<UUID> members = new TreeSet<>();
            String memberText = encoded.get("members");
            if (!memberText.isBlank()) {
                for (String member : memberText.split(",", -1)) {
                    if (!members.add(UUID.fromString(member))) {
                        throw new IllegalArgumentException("Duplicate automation member");
                    }
                }
            }
            String publicText = required(encoded, "public_insert");
            if (!publicText.equals("true") && !publicText.equals("false")) {
                throw new IllegalArgumentException("Invalid public insert flag");
            }
            return new AutomationAccess(owner, members, Boolean.parseBoolean(publicText));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed automation access state", exception);
        }
    }

    private static String required(Map<String, String> encoded, String key) {
        String value = encoded.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing automation access field: " + key);
        }
        return value;
    }
}
