package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Prevents maintained locales from silently losing or retaining obsolete keys. */
class LocalizationParityTest {
    @Test
    void englishAndTurkishHaveExactKeyParity() throws IOException {
        JsonObject english = read("src/main/generated/assets/projectex/lang/en_us.json");
        JsonObject turkish = read("src/main/resources/assets/projectex/lang/tr_tr.json");

        assertEquals(new TreeSet<>(english.keySet()), new TreeSet<>(turkish.keySet()),
            "en_us and tr_tr must contain exactly the same translation keys");
    }

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }
}
