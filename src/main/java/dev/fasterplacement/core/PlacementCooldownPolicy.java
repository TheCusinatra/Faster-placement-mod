package dev.fasterplacement.core;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public final class PlacementCooldownPolicy {
    private long nextMainAllowedTick;
    private long nextOffAllowedTick;

    public boolean canAttempt(ClientPlayerEntity player, Hand hand, ItemStack stack, long nowTick) {
        if (stack.isEmpty()) {
            return false;
        }

        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return false;
        }

        return nowTick >= nextAllowedTick(hand);
    }

    public void onAccepted(Hand hand, long nowTick, PlacementSmoothingMode mode) {
        long delay = switch (mode) {
            case STRICT -> 2L;
            case BALANCED -> 1L;
            case AGGRESSIVE -> 1L;
        };
        setNextAllowedTick(hand, nowTick + delay);
    }

    public void onRejected(Hand hand, long nowTick, PlacementSmoothingMode mode) {
        if (mode == PlacementSmoothingMode.STRICT) {
            setNextAllowedTick(hand, nowTick + 1L);
        }
    }

    public void reset() {
        nextMainAllowedTick = 0L;
        nextOffAllowedTick = 0L;
    }

    private long nextAllowedTick(Hand hand) {
        return hand == Hand.OFF_HAND ? nextOffAllowedTick : nextMainAllowedTick;
    }

    private void setNextAllowedTick(Hand hand, long tick) {
        if (hand == Hand.OFF_HAND) {
            nextOffAllowedTick = tick;
        } else {
            nextMainAllowedTick = tick;
        }
    }
}
