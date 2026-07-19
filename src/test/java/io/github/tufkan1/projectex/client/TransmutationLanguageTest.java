package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class TransmutationLanguageTest {
    @Test
    void everyServerFailureHasEnglishAndTurkishScreenText() throws Exception {
        JsonObject english = language("en_us");
        JsonObject turkish = language("tr_tr");

        for (AlchemyTransactionFailure failure : AlchemyTransactionFailure.values()) {
            String key = "screen.projectex.failure." + failure.name().toLowerCase(Locale.ROOT);
            assertTrue(english.has(key), "Missing English key: " + key);
            assertTrue(turkish.has(key), "Missing Turkish key: " + key);
        }
        assertEquals(english.keySet(), turkish.keySet(),
            "English and Turkish language keys must stay in sync");
    }

    private static JsonObject language(String locale) throws Exception {
        String path = "/assets/projectex/lang/" + locale + ".json";
        try (var stream = TransmutationLanguageTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing language resource: " + path);
            }
            return JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
