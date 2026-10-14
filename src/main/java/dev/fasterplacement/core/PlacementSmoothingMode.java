package dev.fasterplacement.core;

import dev.fasterplacement.config.PlacementConfig;

public enum PlacementSmoothingMode {
    STRICT,
    BALANCED,
    AGGRESSIVE;

    public static PlacementSmoothingMode fromConfig(PlacementConfig config) {
        if (config.strictModeEnabled) {
            return STRICT;
        }
        if (config.aggressiveLowFpsModeEnabled) {
            return AGGRESSIVE;
        }
        return BALANCED;
    }
}