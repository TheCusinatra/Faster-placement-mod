package dev.fasterplacement.bridge;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;

public final class PlacementMixinBridge {
    private static final float DUPLICATE_EPSILON = 0.0005f;

    private static long currentClientTick = -1L;
    private static long lastVanillaBlockAttemptTick = -1L;
    private static long lastReplayAcceptedTick = -1L;
    private static long lastReplayBlockPos = Long.MIN_VALUE;
    private static int lastReplaySideOrdinal = -1;
    private static byte lastReplayHandId = -1;
    private static boolean lastReplayInsideBlock;
    private static float lastReplayHitX;
    private static float lastReplayHitY;
    private static float lastReplayHitZ;
    private static boolean replayCallActive;

    private PlacementMixinBridge() {
    }

    public static void onTickStart(long tick) {
        currentClientTick = tick;
    }

    public static void onInteractBlockCall(ClientPlayerEntity player, Hand hand) {
        if (replayCallActive || player == null) {
            return;
        }

        if (player.getStackInHand(hand).getItem() instanceof BlockItem) {
            lastVanillaBlockAttemptTick = currentClientTick;
        }
    }

    public static boolean wasVanillaBlockAttemptThisTick(long tick) {
        return lastVanillaBlockAttemptTick == tick;
    }

    public static void onReplayAccepted(Hand hand, long blockPos, int sideOrdinal, boolean insideBlock,
                                        float hitX, float hitY, float hitZ) {
        lastReplayAcceptedTick = currentClientTick;
        lastReplayBlockPos = blockPos;
        lastReplaySideOrdinal = sideOrdinal;
        lastReplayHandId = (byte) (hand == Hand.OFF_HAND ? 1 : 0);
        lastReplayInsideBlock = insideBlock;
        lastReplayHitX = hitX;
        lastReplayHitY = hitY;
        lastReplayHitZ = hitZ;
    }

    public static boolean shouldCancelVanillaDuplicate(ClientPlayerEntity player, Hand hand, long blockPos, int sideOrdinal,
                                                       boolean insideBlock, float hitX, float hitY, float hitZ) {
        if (replayCallActive || player == null || currentClientTick != lastReplayAcceptedTick) {
            return false;
        }

        if (!(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
            return false;
        }

        if (lastReplayHandId != (byte) (hand == Hand.OFF_HAND ? 1 : 0)) {
            return false;
        }

        return lastReplayBlockPos == blockPos
            && lastReplaySideOrdinal == sideOrdinal
            && lastReplayInsideBlock == insideBlock
            && abs(lastReplayHitX - hitX) <= DUPLICATE_EPSILON
            && abs(lastReplayHitY - hitY) <= DUPLICATE_EPSILON
            && abs(lastReplayHitZ - hitZ) <= DUPLICATE_EPSILON;
    }

    public static boolean wasReplayAcceptedThisTick(Hand hand, long blockPos, int sideOrdinal,
                                                    boolean insideBlock, float hitX, float hitY, float hitZ) {
        if (currentClientTick != lastReplayAcceptedTick) {
            return false;
        }

        if (lastReplayHandId != (byte) (hand == Hand.OFF_HAND ? 1 : 0)) {
            return false;
        }

        return lastReplayBlockPos == blockPos
            && lastReplaySideOrdinal == sideOrdinal
            && lastReplayInsideBlock == insideBlock
            && abs(lastReplayHitX - hitX) <= DUPLICATE_EPSILON
            && abs(lastReplayHitY - hitY) <= DUPLICATE_EPSILON
            && abs(lastReplayHitZ - hitZ) <= DUPLICATE_EPSILON;
    }

    public static void markReplayCallStart() {
        replayCallActive = true;
    }

    public static void markReplayCallEnd() {
        replayCallActive = false;
    }

    public static void reset() {
        currentClientTick = -1L;
        lastVanillaBlockAttemptTick = -1L;
        lastReplayAcceptedTick = -1L;
        lastReplayBlockPos = Long.MIN_VALUE;
        lastReplaySideOrdinal = -1;
        lastReplayHandId = -1;
        lastReplayInsideBlock = false;
        lastReplayHitX = 0.0f;
        lastReplayHitY = 0.0f;
        lastReplayHitZ = 0.0f;
        replayCallActive = false;
    }

    private static float abs(float value) {
        return value < 0.0f ? -value : value;
    }
}
