package dev.fasterplacement.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public final class ClientInputTracker {
    private int rawRightClickPressCounter;
    private int consumedPressCounter;

    private boolean useHeld;
    private boolean pressedThisTick;
    private float yaw;
    private float pitch;
    private float previousYaw;
    private float previousPitch;

    public void onRawRightClickPress() {
        rawRightClickPressCounter++;
    }

    public void beginTick(MinecraftClient client) {
        pressedThisTick = rawRightClickPressCounter != consumedPressCounter;
        consumedPressCounter = rawRightClickPressCounter;

        useHeld = client.options.useKey.isPressed() || pressedThisTick;

        PlayerEntity player = client.player;
        if (player != null) {
            previousYaw = yaw;
            previousPitch = pitch;
            yaw = player.getYaw();
            pitch = player.getPitch();
        }
    }

    public boolean useHeld() {
        return useHeld;
    }

    public boolean pressedThisTick() {
        return pressedThisTick;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float yawDeltaAbs() {
        return wrappedDeltaAbs(yaw, previousYaw);
    }

    public float pitchDeltaAbs() {
        return Math.abs(pitch - previousPitch);
    }

    public float maxAimDeltaAbs() {
        float yawDelta = yawDeltaAbs();
        float pitchDelta = pitchDeltaAbs();
        return yawDelta > pitchDelta ? yawDelta : pitchDelta;
    }

    public void reset() {
        rawRightClickPressCounter = 0;
        consumedPressCounter = 0;
        useHeld = false;
        pressedThisTick = false;
        yaw = 0.0f;
        pitch = 0.0f;
        previousYaw = 0.0f;
        previousPitch = 0.0f;
    }

    private static float wrappedDeltaAbs(float a, float b) {
        float delta = a - b;
        while (delta > 180.0f) {
            delta -= 360.0f;
        }
        while (delta < -180.0f) {
            delta += 360.0f;
        }
        return Math.abs(delta);
    }
}