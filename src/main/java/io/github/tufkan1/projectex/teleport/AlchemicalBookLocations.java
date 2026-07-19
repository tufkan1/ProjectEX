package io.github.tufkan1.projectex.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Immutable destination collection with a separate one-shot back target. */
public record AlchemicalBookLocations(List<AlchemicalDestination> destinations, Optional<AlchemicalDestination> back) {
    public static final int MAX_DESTINATIONS = 64;
    public static final AlchemicalBookLocations EMPTY = new AlchemicalBookLocations(List.of(), Optional.empty());
    public static final Codec<AlchemicalBookLocations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AlchemicalDestination.CODEC.listOf(0, MAX_DESTINATIONS).fieldOf("destinations")
            .forGetter(AlchemicalBookLocations::destinations),
        AlchemicalDestination.CODEC.optionalFieldOf("back").forGetter(AlchemicalBookLocations::back)
    ).apply(instance, AlchemicalBookLocations::new));

    public AlchemicalBookLocations {
        destinations = List.copyOf(destinations);
        if (destinations.size() > MAX_DESTINATIONS) throw new IllegalArgumentException("Too many destinations");
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (AlchemicalDestination destination : destinations) {
            if (!names.add(key(destination.name()))) throw new IllegalArgumentException("Duplicate destination name");
        }
    }

    public Optional<AlchemicalDestination> find(String name) {
        String key = key(name);
        return destinations.stream().filter(value -> key(value.name()).equals(key)).findFirst();
    }

    public AlchemicalBookLocations add(AlchemicalDestination destination) {
        if (find(destination.name()).isPresent()) throw new IllegalArgumentException("Duplicate destination name");
        if (destinations.size() >= MAX_DESTINATIONS) throw new IllegalArgumentException("Too many destinations");
        java.util.ArrayList<AlchemicalDestination> replacement = new java.util.ArrayList<>(destinations);
        replacement.add(destination);
        return new AlchemicalBookLocations(replacement, back);
    }

    public AlchemicalBookLocations remove(String name) {
        String key = key(name);
        List<AlchemicalDestination> replacement = destinations.stream()
            .filter(value -> !key(value.name()).equals(key)).toList();
        if (replacement.size() == destinations.size()) throw new IllegalArgumentException("Unknown destination name");
        return new AlchemicalBookLocations(replacement, back);
    }

    public AlchemicalBookLocations withBack(AlchemicalDestination destination) {
        return new AlchemicalBookLocations(destinations, Optional.of(destination));
    }

    public AlchemicalBookLocations clearBack() {
        return back.isEmpty() ? this : new AlchemicalBookLocations(destinations, Optional.empty());
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
