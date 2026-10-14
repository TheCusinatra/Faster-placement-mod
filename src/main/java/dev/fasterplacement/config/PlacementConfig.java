package dev.fasterplacement.config;

public final class PlacementConfig {
    public boolean enabled = true;
    public int placementQueueSize = 48;
    public boolean aggressiveLowFpsModeEnabled = false;
    public boolean strictModeEnabled = false;
    public boolean offhandEnabled = true;
    public boolean requireSneakingForInteractableBypass = true;
    public float mouseMovementThreshold = 7.5f;
    public boolean debugLoggingEnabled = false;

    public void clamp() {
        if (placementQueueSize < 4) {
            placementQueueSize = 4;
        } else if (placementQueueSize > 256) {
            placementQueueSize = 256;
        }

        if (mouseMovementThreshold < 0.25f) {
            mouseMovementThreshold = 0.25f;
        } else if (mouseMovementThreshold > 45.0f) {
            mouseMovementThreshold = 45.0f;
        }
    }
}