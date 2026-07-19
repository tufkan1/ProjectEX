package io.github.tufkan1.projectex.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable, bounded insertion-filter policy carried by advanced storage block items. */
public record AdvancedStorageConfig(FilterMode filterMode, List<String> itemIds) {
    public static final int MAX_FILTERS = 64;
    public static final AdvancedStorageConfig DEFAULT = new AdvancedStorageConfig(
        FilterMode.ALLOW_ALL, List.of()
    );
    public static final Codec<AdvancedStorageConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.xmap(FilterMode::decode, mode -> mode.name().toLowerCase(Locale.ROOT))
                .optionalFieldOf("filter_mode", FilterMode.ALLOW_ALL)
                .forGetter(AdvancedStorageConfig::filterMode),
            Codec.STRING.listOf(0, MAX_FILTERS).optionalFieldOf("item_filters", List.of())
                .forGetter(AdvancedStorageConfig::itemIds)
        ).apply(instance, AdvancedStorageConfig::new)
    );

    public AdvancedStorageConfig {
        Objects.requireNonNull(filterMode, "filterMode");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String itemId : List.copyOf(itemIds)) {
            String value = Objects.requireNonNull(itemId, "itemId").trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) throw new IllegalArgumentException("Storage filter id cannot be blank");
            normalized.add(value);
        }
        if (normalized.size() > MAX_FILTERS) throw new IllegalArgumentException("Too many storage filters");
        itemIds = normalized.stream().sorted().toList();
    }

    public boolean allows(String itemId) {
        boolean listed = itemIds.contains(itemId.toLowerCase(Locale.ROOT));
        return switch (filterMode) {
            case ALLOW_ALL -> true;
            case ALLOW_LIST -> listed;
            case DENY_LIST -> !listed;
        };
    }

    public AdvancedStorageConfig cycleMode() {
        return new AdvancedStorageConfig(filterMode.next(), itemIds);
    }

    public AdvancedStorageConfig toggle(String itemId) {
        LinkedHashSet<String> changed = new LinkedHashSet<>(itemIds);
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        if (!changed.remove(normalized)) {
            if (changed.size() >= MAX_FILTERS) return this;
            changed.add(normalized);
        }
        return new AdvancedStorageConfig(filterMode, List.copyOf(changed));
    }

    public enum FilterMode {
        ALLOW_ALL, ALLOW_LIST, DENY_LIST;

        public FilterMode next() { return values()[(ordinal() + 1) % values().length]; }

        private static FilterMode decode(String value) {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }
}
