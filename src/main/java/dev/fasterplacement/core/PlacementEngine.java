package dev.fasterplacement.core;

import dev.fasterplacement.FasterPlacementClient;
import dev.fasterplacement.bridge.PlacementMixinBridge;
import dev.fasterplacement.config.PlacementConfig;
import dev.fasterplacement.config.PlacementConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PlacementEngine {
    private static final int DEBUG_LOG_EVERY_TICKS = 200;

    private final PlacementConfigManager configManager;
    private final PlacementQueueManager queueManager;
    private final PlacementValidator validator;
    private final PlacementCooldownPolicy cooldownPolicy;
    private final ClientInputTracker inputTracker;
    private final PlacementDebugMetrics metrics;

    private final PlacementIntent peekIntent = new PlacementIntent();

    private long clientTick;
    private long lastMetricsLogTick;

    public PlacementEngine(PlacementConfigManager configManager,
                           PlacementQueueManager queueManager,
                           PlacementValidator validator,
                           PlacementCooldownPolicy cooldownPolicy,
                           ClientInputTracker inputTracker,
                           PlacementDebugMetrics metrics) {
        this.configManager = configManager;
        this.queueManager = queueManager;
        this.validator = validator;
        this.cooldownPolicy = cooldownPolicy;
        this.inputTracker = inputTracker;
        this.metrics = metrics;
    }

    public void onStartClientTick(MinecraftClient client) {
        clientTick++;
        inputTracker.beginTick(client);
        PlacementMixinBridge.onTickStart(clientTick);
        runStartOfTickFastPath(client);
    }

    public void onEndClientTick(MinecraftClient client) {
        PlacementConfig config = configManager.config();
        if (!config.enabled) {
            queueManager.clear();
            logMetricsIfNeeded(false);
            return;
        }

        queueManager.ensureCapacity(config.placementQueueSize);

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null || client.currentScreen != null) {
            queueManager.clear();
            cooldownPolicy.reset();
            logMetricsIfNeeded(false);
            return;
        }

        PlacementSmoothingMode mode = PlacementSmoothingMode.fromConfig(config);

        if (inputTracker.useHeld()) {
            captureIntentIfPossible(config, player, world, Hand.MAIN_HAND, client.crosshairTarget, mode);
            if (config.offhandEnabled && isOffhandPlacementAllowed(player)) {
                captureIntentIfPossible(config, player, world, Hand.OFF_HAND, client.crosshairTarget, mode);
            }
        }

        if (queueManager.size() == 0) {
            logMetricsIfNeeded(false);
            return;
        }

        // If vanilla already attempted block interaction this tick, replay starts next tick.
        if (!inputTracker.pressedThisTick() && PlacementMixinBridge.wasVanillaBlockAttemptThisTick(clientTick)) {
            logMetricsIfNeeded(false);
            return;
        }

        flushQueue(config, mode, player, world, interactionManager);
        logMetricsIfNeeded(false);
    }

    public void onRawRightClickPress(MinecraftClient client) {
        inputTracker.onRawRightClickPress();

        if (client == null) {
            return;
        }

        PlacementConfig config = configManager.config();
        if (!config.enabled) {
            return;
        }

        queueManager.ensureCapacity(config.placementQueueSize);
        captureCurrentIntent(client, Hand.MAIN_HAND, true);

        ClientPlayerEntity player = client.player;
        if (config.offhandEnabled && player != null && isOffhandPlacementAllowed(player)) {
            captureCurrentIntent(client, Hand.OFF_HAND, true);
        }

        flushAsSoonAsPossible(client, true);
    }

    public void resetSessionState() {
        queueManager.clear();
        cooldownPolicy.reset();
        inputTracker.reset();
        PlacementMixinBridge.reset();
    }

    public void logMetricsIfNeeded(boolean force) {
        PlacementConfig config = configManager.config();
        if (!config.debugLoggingEnabled) {
            return;
        }

        if (!force && clientTick - lastMetricsLogTick < DEBUG_LOG_EVERY_TICKS) {
            return;
        }

        lastMetricsLogTick = clientTick;
        FasterPlacementClient.LOGGER.info("[FasterPlacement] {}", metrics.compactLine(queueManager.size()));
    }

    public void toggleEnabled(MinecraftClient client) {
        PlacementConfig config = configManager.config();
        config.enabled = !config.enabled;

        if (!config.enabled) {
            queueManager.clear();
            cooldownPolicy.reset();
        }

        if (client.player != null) {
            Text message = Text.translatable(
                config.enabled ? "message.fasterplacement.enabled" : "message.fasterplacement.disabled"
            );
            client.player.sendMessage(message, true);
        }
    }

    private void captureIntentIfPossible(PlacementConfig config, ClientPlayerEntity player, ClientWorld world,
                                         Hand hand, Object crosshairTarget, PlacementSmoothingMode mode) {
        ItemStack stack = player.getStackInHand(hand);
        if (!validator.canCaptureFromCrosshair(config, player, world, hand, stack, crosshairTarget)) {
            if (inputTracker.useHeld()) {
                metrics.skippedDueToInvalidTarget++;
            }
            return;
        }

        if (!(crosshairTarget instanceof BlockHitResult hitResult)) {
            return;
        }

        if (PlacementMixinBridge.wasReplayAcceptedThisTick(
            hand,
            hitResult.getBlockPos().asLong(),
            hitResult.getSide().ordinal(),
            hitResult.isInsideBlock(),
            (float) hitResult.getPos().x,
            (float) hitResult.getPos().y,
            (float) hitResult.getPos().z
        )) {
            return;
        }

        // Strict mode drops noisy intent capture to preserve exact vanilla-like feel.
        if (mode == PlacementSmoothingMode.STRICT
            && !inputTracker.pressedThisTick()
            && inputTracker.maxAimDeltaAbs() > config.mouseMovementThreshold) {
            return;
        }

        queueManager.enqueue(hitResult, hand, inputTracker.yaw(), inputTracker.pitch(), clientTick, metrics);
    }

    private void flushQueue(PlacementConfig config,
                            PlacementSmoothingMode mode,
                            ClientPlayerEntity player,
                            ClientWorld world,
                            ClientPlayerInteractionManager interactionManager) {
        int budget = switch (mode) {
            case STRICT -> 1;
            case BALANCED -> 1;
            case AGGRESSIVE -> 2;
        };

        boolean usedMain = false;
        boolean usedOff = false;

        while (budget > 0 && queueManager.peekInto(peekIntent)) {
            Hand hand = peekIntent.hand();

            if ((hand == Hand.MAIN_HAND && usedMain) || (hand == Hand.OFF_HAND && usedOff)) {
                break;
            }

            if (hand == Hand.OFF_HAND && !config.offhandEnabled) {
                queueManager.popHead();
                metrics.skippedDueToInvalidTarget++;
                continue;
            }

            if (hand == Hand.OFF_HAND && !isOffhandPlacementAllowed(player)) {
                queueManager.popHead();
                metrics.skippedDueToInvalidTarget++;
                continue;
            }

            ItemStack stack = player.getStackInHand(hand);
            if (!validator.isIntentStillValid(config, player, world, hand, stack, peekIntent,
                inputTracker.yaw(), inputTracker.pitch(), mode)) {
                queueManager.popHead();
                metrics.skippedDueToInvalidTarget++;
                continue;
            }

            if (!cooldownPolicy.canAttempt(player, hand, stack, clientTick)) {
                metrics.skippedDueToCooldown++;
                break;
            }

            BlockHitResult replayHit = new BlockHitResult(
                new Vec3d(peekIntent.hitX(), peekIntent.hitY(), peekIntent.hitZ()),
                peekIntent.side(),
                BlockPos.fromLong(peekIntent.blockPos()),
                peekIntent.insideBlock()
            );

            PlacementMixinBridge.markReplayCallStart();
            ActionResult result;
            try {
                result = interactionManager.interactBlock(player, hand, replayHit);
            } finally {
                PlacementMixinBridge.markReplayCallEnd();
            }

            if (result.isAccepted()) {
                queueManager.popHead();
                queueManager.discardLeadingDuplicates(
                    peekIntent.blockPos(),
                    peekIntent.sideId(),
                    hand,
                    peekIntent.insideBlock(),
                    peekIntent.hitX(),
                    peekIntent.hitY(),
                    peekIntent.hitZ()
                );
                metrics.flushedPlacements++;

                PlacementMixinBridge.onReplayAccepted(
                    hand,
                    peekIntent.blockPos(),
                    peekIntent.sideId(),
                    peekIntent.insideBlock(),
                    peekIntent.hitX(),
                    peekIntent.hitY(),
                    peekIntent.hitZ()
                );

                if (clientTick - peekIntent.capturedTick() > 1L) {
                    metrics.recoveredPlacementsAfterFpsDrop++;
                }

                cooldownPolicy.onAccepted(hand, clientTick, mode);
            } else {
                queueManager.popHead();
                metrics.skippedDueToInvalidTarget++;
                cooldownPolicy.onRejected(hand, clientTick, mode);
            }

            if (hand == Hand.MAIN_HAND) {
                usedMain = true;
            } else {
                usedOff = true;
            }

            budget--;
        }
    }

    private boolean isOffhandPlacementAllowed(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        return mainHandStack.isEmpty() || validator.isBlockPlacementStack(mainHandStack);
    }

    private void runStartOfTickFastPath(MinecraftClient client) {
        PlacementConfig config = configManager.config();
        if (!config.enabled) {
            return;
        }

        queueManager.ensureCapacity(config.placementQueueSize);
        captureCurrentIntent(client, Hand.MAIN_HAND, false);

        ClientPlayerEntity player = client.player;
        if (config.offhandEnabled && player != null && isOffhandPlacementAllowed(player)) {
            captureCurrentIntent(client, Hand.OFF_HAND, false);
        }

        flushAsSoonAsPossible(client, false);
    }

    private void captureCurrentIntent(MinecraftClient client, Hand hand, boolean allowStrictBypassOnRawPress) {
        PlacementConfig config = configManager.config();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null || client.interactionManager == null || client.currentScreen != null) {
            return;
        }

        PlacementSmoothingMode mode = PlacementSmoothingMode.fromConfig(config);
        if (!allowStrictBypassOnRawPress && !inputTracker.useHeld()) {
            return;
        }

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (!allowStrictBypassOnRawPress) {
            captureIntentIfPossible(config, player, world, hand, client.crosshairTarget, mode);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!validator.canCaptureFromCrosshair(config, player, world, hand, stack, client.crosshairTarget)) {
            return;
        }

        if (client.crosshairTarget instanceof BlockHitResult hitResult) {
            if (PlacementMixinBridge.wasReplayAcceptedThisTick(
                hand,
                hitResult.getBlockPos().asLong(),
                hitResult.getSide().ordinal(),
                hitResult.isInsideBlock(),
                (float) hitResult.getPos().x,
                (float) hitResult.getPos().y,
                (float) hitResult.getPos().z
            )) {
                return;
            }

            queueManager.enqueue(hitResult, hand, yaw, pitch, clientTick, metrics);
        }
    }

    private void flushAsSoonAsPossible(MinecraftClient client, boolean skipIfVanillaAlreadyAttempted) {
        PlacementConfig config = configManager.config();
        if (!config.enabled || queueManager.size() == 0) {
            return;
        }

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null || client.currentScreen != null) {
            return;
        }

        if (skipIfVanillaAlreadyAttempted && PlacementMixinBridge.wasVanillaBlockAttemptThisTick(clientTick)) {
            return;
        }

        flushQueue(config, PlacementSmoothingMode.fromConfig(config), player, world, interactionManager);
    }
}
