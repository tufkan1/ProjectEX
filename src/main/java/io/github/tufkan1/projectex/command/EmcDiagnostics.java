package io.github.tufkan1.projectex.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.tufkan1.projectex.api.emc.EmcApi;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Map;
import java.util.TreeMap;

/** Creates a stable machine-readable view suitable for logs and bug reports. */
public final class EmcDiagnostics {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private EmcDiagnostics() {
    }

    public static String toJson(EmcApi registry) {
        EmcSnapshot snapshot = registry.snapshot();
        Map<EmcMatch, EmcValue> values = snapshot.values();
        Map<EmcMatch, String> sources = snapshot.sources();
        Map<String, Entry> output = new TreeMap<>();
        values.forEach((match, value) -> output.put(match.toString(),
            new Entry(value.amount().toString(), sources.getOrDefault(match, "unknown"))));
        return GSON.toJson(new Report(1, output.size(), output));
    }

    private record Report(int reportVersion, int count, Map<String, Entry> values) {
    }

    private record Entry(String emc, String source) {
    }
}
