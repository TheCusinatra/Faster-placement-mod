package dev.fasterplacement.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.fasterplacement.FasterPlacementClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlacementConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private PlacementConfig config;

    public PlacementConfigManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("fasterplacement-client.json");
        this.config = new PlacementConfig();
    }

    public PlacementConfig config() {
        return config;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            config = new PlacementConfig();
            config.clamp();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            PlacementConfig loaded = GSON.fromJson(reader, PlacementConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Config JSON is empty");
            }

            loaded.clamp();
            config = loaded;
        } catch (IOException | JsonParseException ex) {
            FasterPlacementClient.LOGGER.warn("Failed to load config, using defaults: {}", ex.getMessage());
            config = new PlacementConfig();
            config.clamp();
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            config.clamp();
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ex) {
            FasterPlacementClient.LOGGER.error("Failed to save config: {}", ex.getMessage());
        }
    }
}