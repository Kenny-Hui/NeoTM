package com.lx862.mtrtm.mod.config;

import com.google.gson.*;
import com.lx862.mtrtm.mod.NeoTM;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {
    public static int mtrJourneyPlannerTickTime = 0;
    public static int shearPSDOpLevel = 0;

    public static boolean load(Path path) {
        if(!Files.exists(path.resolve("config.json"))) {
            NeoTM.LOGGER.info("[{}] Config not found, generating one...", NeoTM.BRAND);
            writeConfig(path);
            return true;
        }

        NeoTM.LOGGER.info("[{}] Reading Train Config...", NeoTM.BRAND);
        try {
            final JsonObject jsonConfig = JsonParser.parseString(String.join("", Files.readAllLines(path.resolve("config.json")))).getAsJsonObject();
            try {
                mtrJourneyPlannerTickTime = jsonConfig.get("mtrJourneyPlannerTickTime").getAsInt();
            } catch (Exception ignored) {}

            try {
                shearPSDOpLevel = jsonConfig.get("shearPSDOpLevel").getAsInt();
            } catch (Exception ignored) {}

        } catch (Exception e) {
            NeoTM.LOGGER.error("[{}] Failed to read config!", NeoTM.BRAND, e);
            return false;
        }
        return true;
    }

    public static void writeConfig(Path path) {
        NeoTM.LOGGER.info("[{}] Writing Config...", NeoTM.BRAND);
        final JsonObject jsonConfig = new JsonObject();
        jsonConfig.addProperty("mtrJourneyPlannerTickTime", mtrJourneyPlannerTickTime);
        jsonConfig.addProperty("shearPSDOpLevel", shearPSDOpLevel);

        try {
            Files.createDirectories(path);
            Files.write(path.resolve("config.json"), Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(jsonConfig)));
        } catch (Exception e) {
            NeoTM.LOGGER.error("[{}] Failed to write config!", NeoTM.BRAND, e);
        }
    }
}
