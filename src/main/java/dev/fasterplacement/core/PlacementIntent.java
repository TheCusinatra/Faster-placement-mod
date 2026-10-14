package dev.fasterplacement.core;

import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

public final class PlacementIntent {
    private long blockPos;
    private int sideId;
    private byte handId;
    private boolean insideBlock;
    private float hitX;
    private float hitY;
    private float hitZ;
    private float yaw;
    private float pitch;
    private long capturedTick;

    public void set(long blockPos, int sideId, Hand hand, boolean insideBlock, float hitX, float hitY, float hitZ,
                    float yaw, float pitch, long capturedTick) {
        this.blockPos = blockPos;
        this.sideId = sideId;
        this.handId = (byte) (hand == Hand.OFF_HAND ? 1 : 0);
        this.insideBlock = insideBlock;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.capturedTick = capturedTick;
    }

    public long blockPos() {
        return blockPos;
    }

    public int sideId() {
        return sideId;
    }

    public Direction side() {
        Direction[] directions = Direction.values();
        if (sideId < 0 || sideId >= directions.length) {
            return Direction.UP;
        }
        return directions[sideId];
    }

    public Hand hand() {
        return handId == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public boolean insideBlock() {
        return insideBlock;
    }

    public float hitX() {
        return hitX;
    }

    public float hitY() {
        return hitY;
    }

    public float hitZ() {
        return hitZ;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public long capturedTick() {
        return capturedTick;
    }
}
