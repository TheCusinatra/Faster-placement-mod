package dev.fasterplacement.core;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public final class PlacementQueueManager {
    private static final float DUPLICATE_EPSILON = 0.0005f;

    private long[] blockPosLong;
    private int[] sideId;
    private byte[] handId;
    private boolean[] insideBlock;
    private float[] hitX;
    private float[] hitY;
    private float[] hitZ;
    private float[] yaw;
    private float[] pitch;
    private long[] capturedTick;

    private int head;
    private int size;

    public PlacementQueueManager(int capacity) {
        int normalized = normalizeCapacity(capacity);
        allocate(normalized);
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return blockPosLong.length;
    }

    public void ensureCapacity(int requestedCapacity) {
        int normalized = normalizeCapacity(requestedCapacity);
        if (normalized == capacity()) {
            return;
        }

        long[] oldPos = blockPosLong;
        int[] oldSide = sideId;
        byte[] oldHand = handId;
        boolean[] oldInside = insideBlock;
        float[] oldHitX = hitX;
        float[] oldHitY = hitY;
        float[] oldHitZ = hitZ;
        float[] oldYaw = yaw;
        float[] oldPitch = pitch;
        long[] oldCaptured = capturedTick;

        int oldCapacity = oldPos.length;
        int keep = Math.min(size, normalized);
        int startOffset = size - keep;

        allocate(normalized);

        for (int i = 0; i < keep; i++) {
            int oldIndex = (head + startOffset + i) % oldCapacity;
            blockPosLong[i] = oldPos[oldIndex];
            sideId[i] = oldSide[oldIndex];
            handId[i] = oldHand[oldIndex];
            insideBlock[i] = oldInside[oldIndex];
            hitX[i] = oldHitX[oldIndex];
            hitY[i] = oldHitY[oldIndex];
            hitZ[i] = oldHitZ[oldIndex];
            yaw[i] = oldYaw[oldIndex];
            pitch[i] = oldPitch[oldIndex];
            capturedTick[i] = oldCaptured[oldIndex];
        }

        head = 0;
        size = keep;
    }

    public boolean enqueue(BlockHitResult hitResult, Hand hand, float playerYaw, float playerPitch, long tick,
                           PlacementDebugMetrics metrics) {
        return enqueue(hitResult.getBlockPos().asLong(), hitResult.getSide().ordinal(), hand, hitResult.isInsideBlock(),
            (float) hitResult.getPos().x, (float) hitResult.getPos().y, (float) hitResult.getPos().z,
            playerYaw, playerPitch, tick, metrics);
    }

    public boolean enqueue(long targetPos, int targetSideId, Hand hand, boolean isInsideBlock,
                           float targetHitX, float targetHitY, float targetHitZ,
                           float playerYaw, float playerPitch, long tick,
                           PlacementDebugMetrics metrics) {
        if (size > 0 && isDuplicateWithTail(targetPos, targetSideId, hand, targetHitX, targetHitY, targetHitZ)) {
            metrics.rejectedDuplicates++;
            return false;
        }

        if (size == capacity()) {
            head = (head + 1) % capacity();
            size--;
        }

        int index = indexForTailInsert();
        blockPosLong[index] = targetPos;
        sideId[index] = targetSideId;
        handId[index] = (byte) (hand == Hand.OFF_HAND ? 1 : 0);
        insideBlock[index] = isInsideBlock;
        hitX[index] = targetHitX;
        hitY[index] = targetHitY;
        hitZ[index] = targetHitZ;
        yaw[index] = playerYaw;
        pitch[index] = playerPitch;
        capturedTick[index] = tick;
        size++;

        metrics.queuedPlacementIntents++;
        return true;
    }

    public boolean peekInto(PlacementIntent outIntent) {
        if (size == 0) {
            return false;
        }

        int index = head;
        outIntent.set(
            blockPosLong[index],
            sideId[index],
            handId[index] == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND,
            insideBlock[index],
            hitX[index],
            hitY[index],
            hitZ[index],
            yaw[index],
            pitch[index],
            capturedTick[index]
        );
        return true;
    }

    public void popHead() {
        if (size == 0) {
            return;
        }

        head = (head + 1) % capacity();
        size--;
    }

    public void discardLeadingDuplicates(long targetPos, int targetSideId, Hand hand,
                                         boolean isInsideBlock, float targetHitX, float targetHitY, float targetHitZ) {
        while (size > 0) {
            int index = head;
            if (blockPosLong[index] != targetPos || sideId[index] != targetSideId) {
                return;
            }

            byte targetHandId = (byte) (hand == Hand.OFF_HAND ? 1 : 0);
            if (handId[index] != targetHandId || insideBlock[index] != isInsideBlock) {
                return;
            }

            if (abs(hitX[index] - targetHitX) > DUPLICATE_EPSILON
                || abs(hitY[index] - targetHitY) > DUPLICATE_EPSILON
                || abs(hitZ[index] - targetHitZ) > DUPLICATE_EPSILON) {
                return;
            }

            popHead();
        }
    }

    public void clear() {
        head = 0;
        size = 0;
    }

    private boolean isDuplicateWithTail(long targetPos, int targetSideId, Hand hand,
                                        float targetHitX, float targetHitY, float targetHitZ) {
        int tail = (head + size - 1) % capacity();
        if (blockPosLong[tail] != targetPos || sideId[tail] != targetSideId) {
            return false;
        }

        byte targetHandId = (byte) (hand == Hand.OFF_HAND ? 1 : 0);
        if (handId[tail] != targetHandId) {
            return false;
        }

        return abs(hitX[tail] - targetHitX) <= DUPLICATE_EPSILON
            && abs(hitY[tail] - targetHitY) <= DUPLICATE_EPSILON
            && abs(hitZ[tail] - targetHitZ) <= DUPLICATE_EPSILON;
    }

    private int indexForTailInsert() {
        return (head + size) % capacity();
    }

    private static float abs(float value) {
        return value < 0.0f ? -value : value;
    }

    private static int normalizeCapacity(int capacity) {
        if (capacity < 4) {
            return 4;
        }
        if (capacity > 256) {
            return 256;
        }
        return capacity;
    }

    private void allocate(int capacity) {
        blockPosLong = new long[capacity];
        sideId = new int[capacity];
        handId = new byte[capacity];
        insideBlock = new boolean[capacity];
        hitX = new float[capacity];
        hitY = new float[capacity];
        hitZ = new float[capacity];
        yaw = new float[capacity];
        pitch = new float[capacity];
        capturedTick = new long[capacity];
        head = 0;
        size = 0;
    }
}
