package dev.fasterplacement.core;

import dev.fasterplacement.config.PlacementConfig;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SmithingTableBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public final class PlacementValidator {
    // ClassValue gives a permanent class capability cache without reflection in hot paths.
    private static final ClassValue<Boolean> BLOCK_ITEM_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return BlockItem.class.isAssignableFrom(type);
        }
    };

    private static final ClassValue<Boolean> INTERACTABLE_BLOCK_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return DoorBlock.class.isAssignableFrom(type)
                || TrapdoorBlock.class.isAssignableFrom(type)
                || FenceGateBlock.class.isAssignableFrom(type)
                || LeverBlock.class.isAssignableFrom(type)
                || ButtonBlock.class.isAssignableFrom(type)
                || BedBlock.class.isAssignableFrom(type)
                || ChestBlock.class.isAssignableFrom(type)
                || EnderChestBlock.class.isAssignableFrom(type)
                || BarrelBlock.class.isAssignableFrom(type)
                || ShulkerBoxBlock.class.isAssignableFrom(type)
                || CraftingTableBlock.class.isAssignableFrom(type)
                || AbstractFurnaceBlock.class.isAssignableFrom(type)
                || DispenserBlock.class.isAssignableFrom(type)
                || DropperBlock.class.isAssignableFrom(type)
                || HopperBlock.class.isAssignableFrom(type)
                || BrewingStandBlock.class.isAssignableFrom(type)
                || EnchantingTableBlock.class.isAssignableFrom(type)
                || LoomBlock.class.isAssignableFrom(type)
                || CartographyTableBlock.class.isAssignableFrom(type)
                || SmithingTableBlock.class.isAssignableFrom(type)
                || StonecutterBlock.class.isAssignableFrom(type)
                || GrindstoneBlock.class.isAssignableFrom(type)
                || AnvilBlock.class.isAssignableFrom(type)
                || BeaconBlock.class.isAssignableFrom(type)
                || BellBlock.class.isAssignableFrom(type)
                || LecternBlock.class.isAssignableFrom(type)
                || JukeboxBlock.class.isAssignableFrom(type)
                || NoteBlock.class.isAssignableFrom(type)
                || CommandBlock.class.isAssignableFrom(type)
                || RespawnAnchorBlock.class.isAssignableFrom(type)
                || CrafterBlock.class.isAssignableFrom(type)
                || CakeBlock.class.isAssignableFrom(type)
                || BlockWithEntity.class.isAssignableFrom(type);
        }
    };

    public boolean isBlockPlacementStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        return BLOCK_ITEM_CACHE.get(item.getClass());
    }

    public boolean canCaptureFromCrosshair(PlacementConfig config, ClientPlayerEntity player, ClientWorld world,
                                           Hand hand, ItemStack stack, Object crosshairTarget) {
        if (!isBlockPlacementStack(stack)) {
            return false;
        }

        if (!(crosshairTarget instanceof BlockHitResult hitResult)) {
            return false;
        }

        return isTargetPlacementSafe(config, player, world, hand, hitResult);
    }

    public boolean isIntentStillValid(PlacementConfig config, ClientPlayerEntity player, ClientWorld world,
                                      Hand hand, ItemStack stack, PlacementIntent intent,
                                      float currentYaw, float currentPitch,
                                      PlacementSmoothingMode mode) {
        if (!isBlockPlacementStack(stack)) {
            return false;
        }

        BlockPos targetPos = BlockPos.fromLong(intent.blockPos());
        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir()) {
            return false;
        }

        if (config.requireSneakingForInteractableBypass
            && isInteractable(targetState.getBlock())
            && !player.isSneaking()) {
            return false;
        }

        float maxAllowedDelta = mode == PlacementSmoothingMode.AGGRESSIVE
            ? config.mouseMovementThreshold * 1.6f
            : config.mouseMovementThreshold;

        float yawDelta = wrappedDeltaAbs(currentYaw, intent.yaw());
        float pitchDelta = abs(currentPitch - intent.pitch());
        float maxDelta = yawDelta > pitchDelta ? yawDelta : pitchDelta;
        return maxDelta <= maxAllowedDelta;
    }

    private boolean isTargetPlacementSafe(PlacementConfig config, ClientPlayerEntity player, ClientWorld world,
                                          Hand hand, BlockHitResult hitResult) {
        if (hand == Hand.OFF_HAND && !config.offhandEnabled) {
            return false;
        }

        BlockState hitState = world.getBlockState(hitResult.getBlockPos());
        if (hitState.isAir()) {
            return false;
        }

        if (config.requireSneakingForInteractableBypass
            && isInteractable(hitState.getBlock())
            && !player.isSneaking()) {
            return false;
        }

        return true;
    }

    private static boolean isInteractable(Block block) {
        return INTERACTABLE_BLOCK_CACHE.get(block.getClass());
    }

    private static float abs(float value) {
        return value < 0.0f ? -value : value;
    }

    private static float wrappedDeltaAbs(float current, float previous) {
        float delta = current - previous;
        while (delta > 180.0f) {
            delta -= 360.0f;
        }
        while (delta < -180.0f) {
            delta += 360.0f;
        }
        return abs(delta);
    }
}
