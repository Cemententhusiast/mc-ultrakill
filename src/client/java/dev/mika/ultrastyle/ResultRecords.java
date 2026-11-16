package dev.mika.ultrastyle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/** Personal bests are grouped by boss type (or life), difficulty and measurement mode. */
public final class ResultRecords {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("ultrastyle-records.json");
    private Map<String, Long> best = new HashMap<>();
    public ResultRecords() {
        try {
            if (Files.exists(path)) {
                Map<String, Long> loaded = GSON.fromJson(Files.readString(path), new TypeToken<Map<String, Long>>() {}.getType());
                if (loaded != null) loaded.forEach((key, value) -> { if (value != null && value >= 0) best.put(key, value); });
            }
        } catch (Exception e) { LoggerFactory.getLogger("ultrastyle").warn("Unable to load personal bests", e); }
    }
    public long record(CombatResults.Result result) {
        String key = result.key() + "|" + result.difficulty() + "|" + (result.exact() ? "local" : "observed");
        long previous = best.getOrDefault(key, 0L);
        if (!best.containsKey(key) || result.score() > previous) {
            best.put(key, result.score());
            try {
                Files.createDirectories(path.getParent());
                Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
                Files.writeString(temporary, GSON.toJson(best));
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) { LoggerFactory.getLogger("ultrastyle").warn("Unable to save personal bests", e); }
        }
        return Math.max(previous, result.score());
    }
}
