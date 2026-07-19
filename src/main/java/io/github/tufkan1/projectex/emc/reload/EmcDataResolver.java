package io.github.tufkan1.projectex.emc.reload;

import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.emc.data.EmcDataException;
import io.github.tufkan1.projectex.emc.data.EmcDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Resolves priority, conflicts, removals, and aliases without depending on input order. */
public final class EmcDataResolver {
    private EmcDataResolver() {
    }

    public static ResolvedEmcData resolve(List<EmcDataSource> sources) {
        Map<EmcMatch, List<Candidate>> grouped = new TreeMap<>();
        for (EmcDataSource source : sources) {
            for (EmcDefinition definition : source.data().values()) {
                grouped.computeIfAbsent(definition.match(), ignored -> new ArrayList<>())
                    .add(new Candidate(source.sourceId(), source.data().priority(), definition));
            }
        }

        Map<EmcMatch, Candidate> winners = new TreeMap<>();
        grouped.forEach((match, candidates) -> winners.put(match, chooseWinner(match, candidates)));

        Map<EmcMatch, EmcValue> values = new TreeMap<>();
        Map<EmcMatch, String> provenance = new TreeMap<>();
        Set<EmcMatch> visiting = new HashSet<>();
        for (EmcMatch match : winners.keySet()) {
            resolveValue(match, winners, values, provenance, visiting);
        }
        return new ResolvedEmcData(values, provenance);
    }

    private static Candidate chooseWinner(EmcMatch match, List<Candidate> candidates) {
        int highestPriority = candidates.stream().mapToInt(Candidate::priority).max().orElseThrow();
        List<Candidate> highest = candidates.stream()
            .filter(candidate -> candidate.priority() == highestPriority)
            .sorted(Comparator.comparing(Candidate::sourceId))
            .toList();
        EmcDefinition expected = highest.getFirst().definition();
        boolean conflict = highest.stream().anyMatch(candidate -> !sameOperation(expected, candidate.definition()));
        if (conflict) {
            String conflictingSources = highest.stream().map(Candidate::sourceId).sorted().toList().toString();
            throw new EmcDataException("Conflicting EMC definitions for " + match + " at priority "
                + highestPriority + " from " + conflictingSources);
        }
        return highest.getFirst();
    }

    private static boolean sameOperation(EmcDefinition left, EmcDefinition right) {
        return left.kind() == right.kind()
            && java.util.Objects.equals(left.value(), right.value())
            && java.util.Objects.equals(left.alias(), right.alias());
    }

    private static EmcValue resolveValue(
        EmcMatch match,
        Map<EmcMatch, Candidate> winners,
        Map<EmcMatch, EmcValue> resolved,
        Map<EmcMatch, String> provenance,
        Set<EmcMatch> visiting
    ) {
        if (resolved.containsKey(match)) {
            return resolved.get(match);
        }
        Candidate winner = winners.get(match);
        if (winner == null) {
            throw new EmcDataException("Alias target has no EMC definition: " + match);
        }
        if (winner.definition().kind() == EmcDefinition.Kind.REMOVE) {
            return null;
        }
        if (!visiting.add(match)) {
            throw new EmcDataException("Alias cycle detected at " + match);
        }
        try {
            EmcValue value;
            if (winner.definition().kind() == EmcDefinition.Kind.VALUE) {
                value = winner.definition().value();
            } else {
                EmcMatch target = EmcMatch.item(winner.definition().alias());
                value = resolveValue(target, winners, resolved, provenance, visiting);
                if (value == null) {
                    throw new EmcDataException("Alias target is removed: " + target);
                }
            }
            resolved.put(match, value);
            provenance.put(match, winner.sourceId());
            return value;
        } finally {
            visiting.remove(match);
        }
    }

    private record Candidate(String sourceId, int priority, EmcDefinition definition) {
    }
}
