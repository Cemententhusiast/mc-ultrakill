package dev.mika.ultrastyle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StyleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ultrastyle.json");
    public boolean hudEnabled = true;
    public boolean bossBarsEnabled = true;
    public boolean styleXpEnabled = true;
    public float hudScale = 1;
    public float hudTiltDegrees = -4;
    public int rightMargin = 12;
    public int verticalOffset = 0;

    public static StyleConfig load() {
        try {
            if (Files.exists(PATH)) {
                StyleConfig config = GSON.fromJson(Files.readString(PATH), StyleConfig.class);
                if (config == null) throw new IllegalArgumentException("Empty configuration");
                config.sanitize();
                return config;
            }
        } catch (Exception e) {
            LoggerFactory.getLogger("ultrastyle").warn("Cannot read config; using defaults without overwriting the file", e);
            return new StyleConfig();
        }
        StyleConfig config = new StyleConfig();
        config.save();
        return config;
    }
    private void sanitize() {
        if (!Float.isFinite(hudScale)) hudScale = 1;
        if (!Float.isFinite(hudTiltDegrees)) hudTiltDegrees = -4;
        hudScale = Math.clamp(hudScale, 0.5f, 2f);
        hudTiltDegrees = Math.clamp(hudTiltDegrees, -15f, 15f);
        rightMargin = Math.clamp(rightMargin, 0, 500);
        verticalOffset = Math.clamp(verticalOffset, -500, 500);
    }
    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (Exception e) {
            LoggerFactory.getLogger("ultrastyle").warn("Cannot save config", e);
        }
    }
}
