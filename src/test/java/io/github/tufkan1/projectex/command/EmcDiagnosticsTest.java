package io.github.tufkan1.projectex.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmcDiagnosticsTest {
    @Test
    void producesStableMachineReadableJson() {
        EmcMatch diamond = EmcMatch.item(EmcKey.parse("minecraft:diamond"));
        EmcMatch coal = EmcMatch.item(EmcKey.parse("minecraft:coal"));
        Map<EmcMatch, EmcValue> values = new LinkedHashMap<>();
        values.put(diamond, EmcValue.of(8192));
        values.put(coal, EmcValue.of(128));
        EmcValueRegistry registry = new EmcValueRegistry();
        registry.replaceAll(values, Map.of(diamond, "pack-z", coal, "pack-a"));

        String report = EmcDiagnostics.toJson(registry);
        JsonObject root = JsonParser.parseString(report).getAsJsonObject();

        assertEquals(1, root.get("reportVersion").getAsInt());
        assertEquals(2, root.get("count").getAsInt());
        assertEquals("128", root.getAsJsonObject("values")
            .getAsJsonObject("minecraft:coal").get("emc").getAsString());
        assertTrue(report.indexOf("minecraft:coal") < report.indexOf("minecraft:diamond"));
    }
}
