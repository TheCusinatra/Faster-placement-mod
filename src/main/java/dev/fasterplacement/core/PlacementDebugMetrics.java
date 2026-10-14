package dev.fasterplacement.core;

public final class PlacementDebugMetrics {
    public long queuedPlacementIntents;
    public long flushedPlacements;
    public long rejectedDuplicates;
    public long skippedDueToCooldown;
    public long skippedDueToInvalidTarget;
    public long recoveredPlacementsAfterFpsDrop;

    public void reset() {
        queuedPlacementIntents = 0L;
        flushedPlacements = 0L;
        rejectedDuplicates = 0L;
        skippedDueToCooldown = 0L;
        skippedDueToInvalidTarget = 0L;
        recoveredPlacementsAfterFpsDrop = 0L;
    }

    public String compactLine(int queueSize) {
        return "queue=" + queueSize
            + " queued=" + queuedPlacementIntents
            + " flushed=" + flushedPlacements
            + " dedup=" + rejectedDuplicates
            + " cooldownSkip=" + skippedDueToCooldown
            + " invalidSkip=" + skippedDueToInvalidTarget
            + " recovered=" + recoveredPlacementsAfterFpsDrop;
    }
}