package dev.fasterplacement;

import dev.fasterplacement.client.PlacementKeyBindings;
import dev.fasterplacement.config.PlacementConfigManager;
import dev.fasterplacement.core.ClientInputTracker;
import dev.fasterplacement.core.PlacementCooldownPolicy;
import dev.fasterplacement.core.PlacementDebugMetrics;
import dev.fasterplacement.core.PlacementEngine;
import dev.fasterplacement.core.PlacementQueueManager;
import dev.fasterplacement.core.PlacementValidator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FasterPlacementClient implements ClientModInitializer {
    public static final String MOD_ID = "fasterplacement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PlacementEngine engine;

    @Override
    public void onInitializeClient() {
        PlacementConfigManager configManager = new PlacementConfigManager();
        configManager.load();
        PlacementKeyBindings.register();

        PlacementDebugMetrics metrics = new PlacementDebugMetrics();
        PlacementQueueManager queueManager = new PlacementQueueManager(configManager.config().placementQueueSize);
        PlacementValidator validator = new PlacementValidator();
        PlacementCooldownPolicy cooldownPolicy = new PlacementCooldownPolicy();
        ClientInputTracker inputTracker = new ClientInputTracker();

        engine = new PlacementEngine(configManager, queueManager, validator, cooldownPolicy, inputTracker, metrics);

        ClientTickEvents.START_CLIENT_TICK.register(engine::onStartClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PlacementKeyBindings.toggleBinding().wasPressed()) {
                engine.toggleEnabled(client);
                configManager.save();
            }

            engine.onEndClientTick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> engine.resetSessionState());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            engine.logMetricsIfNeeded(true);
            configManager.save();
        });

        LOGGER.info("Faster Placement initialized");
    }

    public static PlacementEngine engine() {
        return engine;
    }
}
